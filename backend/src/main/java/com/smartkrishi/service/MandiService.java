package com.smartkrishi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartkrishi.dto.MandiNearbyResponse;
import com.smartkrishi.dto.MandiNearbyResponse.LocationInfo;
import com.smartkrishi.dto.MandiPriceDto;
import com.smartkrishi.entity.MarketPrice;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.repository.MarketPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MandiService {

    private static final Logger logger = LoggerFactory.getLogger(MandiService.class);

    private final MarketPriceRepository marketPriceRepository;
    private final MandiLocationRegistry mandiLocationRegistry;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${smartkrishi.mandi.api-url:https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070}")
    private String mandiApiUrl;

    @Value("${smartkrishi.mandi.api-key:}")
    private String mandiApiKey;

    @Value("${smartkrishi.mandi.cache-duration-minutes:15}")
    private int cacheDurationMinutes;

    // In-memory cache for external government API calls
    private static class CacheEntry {
        final List<MandiPriceDto> data;
        final long timestamp;

        CacheEntry(List<MandiPriceDto> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }

    private final Map<String, CacheEntry> apiCache = new ConcurrentHashMap<>();
    private LocalDateTime lastGovApiSync = null;

    public MandiService(MarketPriceRepository marketPriceRepository,
                        MandiLocationRegistry mandiLocationRegistry,
                        RestTemplateBuilder restTemplateBuilder) {
        this.marketPriceRepository = marketPriceRepository;
        this.mandiLocationRegistry = mandiLocationRegistry;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retrieves nearby mandi prices using farmer coordinates or manual State/District filters.
     * Follows the strict fallback hierarchy:
     * 1. Official Government data.gov.in / AGMARKNET API (when API key is configured & reachable)
     * 2. Cached Government Data
     * 3. Verified Smart Krishi Database Market Records (transparently labeled)
     */
    public MandiNearbyResponse getNearbyMandiPrices(Double latitude,
                                                    Double longitude,
                                                    Double radiusKm,
                                                    String crop,
                                                    String state,
                                                    String district,
                                                    String sortBy) {
        // Validate coordinates
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new BadRequestException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new BadRequestException("Longitude must be between -180 and 180 degrees.");
        }
        if (radiusKm != null && (radiusKm < 1.0 || radiusKm > 200.0)) {
            throw new BadRequestException("Search radius must be between 1 km and 200 km.");
        }

        double radius = radiusKm != null ? radiusKm : 50.0;
        String cleanCrop = (crop != null && !crop.equalsIgnoreCase("ALL") && !crop.equalsIgnoreCase("All Crops"))
                ? crop.trim() : null;
        String cleanState = (state != null && !state.isBlank() && !state.equalsIgnoreCase("All States"))
                ? state.trim() : null;
        String cleanDistrict = (district != null && !district.isBlank() && !district.equalsIgnoreCase("All Districts"))
                ? district.trim() : null;
        String sortOption = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "nearest";

        MandiNearbyResponse response = new MandiNearbyResponse();
        response.setRadiusKm(radius);
        response.setCropFilter(cleanCrop != null ? cleanCrop : "All Crops");

        // Format resolved location string
        String resolvedLocation = null;
        if (latitude != null && longitude != null) {
            resolvedLocation = String.format("%.4f° N, %.4f° E", latitude, longitude);
            if (cleanDistrict != null && cleanState != null) {
                resolvedLocation += String.format(" (%s, %s)", cleanDistrict, cleanState);
            }
        } else if (cleanDistrict != null || cleanState != null) {
            resolvedLocation = (cleanDistrict != null ? cleanDistrict + ", " : "") + (cleanState != null ? cleanState : "");
        }
        response.setLocation(new LocationInfo(latitude, longitude, resolvedLocation));

        // 1. Try Government API (data.gov.in / AGMARKNET)
        List<MandiPriceDto> govPrices = null;
        boolean fromCache = false;

        if (mandiApiKey != null && !mandiApiKey.trim().isBlank()) {
            String cacheKey = buildCacheKey(cleanCrop, cleanState, cleanDistrict);
            CacheEntry cached = apiCache.get(cacheKey);
            long ttl = cacheDurationMinutes * 60L * 1000L;

            if (cached != null && !cached.isExpired(ttl)) {
                govPrices = cached.data;
                fromCache = true;
                logger.info("Serving mandi prices from memory cache for key: {}", cacheKey);
            } else {
                try {
                    govPrices = fetchFromGovernmentApi(cleanCrop, cleanState, cleanDistrict);
                    if (govPrices != null && !govPrices.isEmpty()) {
                        apiCache.put(cacheKey, new CacheEntry(govPrices));
                        lastGovApiSync = LocalDateTime.now();
                        logger.info("Successfully fetched and cached {} records from data.gov.in", govPrices.size());
                    }
                } catch (Exception e) {
                    logger.warn("Government Mandi API call failed: {}. Checking cache or database fallback.", e.getMessage());
                    if (cached != null) {
                        govPrices = cached.data;
                        fromCache = true;
                    }
                }
            }
        } else {
            logger.info("MANDI_API_KEY is not configured. Utilizing Smart Krishi agricultural repository as primary data source.");
        }

        List<MandiPriceDto> finalResults;

        if (govPrices != null && !govPrices.isEmpty()) {
            // Compute distance for government records
            finalResults = processAndFilterRecords(govPrices, latitude, longitude, radius, cleanCrop, cleanState, cleanDistrict);
            response.setSource("Government of India / AGMARKNET");
            if (fromCache) {
                response.setSourceType("CACHED_GOVERNMENT");
                response.setSourceLabel("Cached Government Mandi Data");
                response.setNotice("Showing cached mandi prices from the official Government of India AGMARKNET portal.");
            } else {
                response.setSourceType("GOVERNMENT_API");
                response.setSourceLabel("Latest Government Mandi Data");
                response.setNotice("Latest official commodity prices retrieved directly from the Government of India Open Data platform.");
            }
            response.setLastUpdated(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(lastGovApiSync != null ? lastGovApiSync : LocalDateTime.now()));
        } else {
            // 3. Fallback to Smart Krishi Admin / Seeded Database
            List<MandiPriceDto> dbPrices = fetchFromDatabase(cleanCrop, cleanState, cleanDistrict);
            finalResults = processAndFilterRecords(dbPrices, latitude, longitude, radius, cleanCrop, cleanState, cleanDistrict);
            response.setSource("Smart Krishi Agricultural Repository");
            response.setSourceType("ADMIN_DATABASE");
            response.setSourceLabel("Smart Krishi Admin Data");
            response.setLastUpdated(DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDateTime.now()));
            if (mandiApiKey == null || mandiApiKey.trim().isBlank()) {
                response.setNotice("Live government data is currently not connected (set MANDI_API_KEY). Showing verified Smart Krishi database market prices.");
            } else {
                response.setNotice("Live government mandi service is temporarily unreachable. Showing verified Smart Krishi database market prices.");
            }
        }

        // Apply sorting
        sortResults(finalResults, sortOption, latitude != null && longitude != null);

        response.setResults(finalResults);
        return response;
    }

    private String buildCacheKey(String crop, String state, String district) {
        return (crop != null ? crop.toLowerCase() : "all") + "_" +
               (state != null ? state.toLowerCase() : "all") + "_" +
               (district != null ? district.toLowerCase() : "all");
    }

    /**
     * Calls data.gov.in AGMARKNET REST API.
     */
    private List<MandiPriceDto> fetchFromGovernmentApi(String crop, String state, String district) {
        StringBuilder urlBuilder = new StringBuilder(mandiApiUrl.trim());
        urlBuilder.append("?api-key=").append(mandiApiKey.trim());
        urlBuilder.append("&format=json&limit=100");

        if (state != null) {
            urlBuilder.append("&filters[state]=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        if (district != null) {
            urlBuilder.append("&filters[district]=").append(URLEncoder.encode(district, StandardCharsets.UTF_8));
        }
        if (crop != null) {
            urlBuilder.append("&filters[commodity]=").append(URLEncoder.encode(crop, StandardCharsets.UTF_8));
        }

        String requestUrl = urlBuilder.toString();
        logger.info("Dispatching query to data.gov.in Mandi API (filters: crop={}, state={}, district={})", crop, state, district);

        String jsonResponse = restTemplate.getForObject(requestUrl, String.class);
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return Collections.emptyList();
        }

        List<MandiPriceDto> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode records = root.path("records");

            if (records.isArray()) {
                long idSeq = 1;
                for (JsonNode node : records) {
                    MandiPriceDto dto = new MandiPriceDto();
                    dto.setId(idSeq++);
                    dto.setState(node.path("state").asText(""));
                    dto.setDistrict(node.path("district").asText(""));
                    dto.setMandiName(node.path("market").asText(""));
                    dto.setCommodity(node.path("commodity").asText(""));
                    dto.setVariety(node.path("variety").asText("Standard"));
                    dto.setGrade(node.path("grade").asText("FAQ"));
                    dto.setArrivalDate(node.path("arrival_date").asText(""));

                    double min = parsePrice(node.path("min_price").asText("0"));
                    double max = parsePrice(node.path("max_price").asText("0"));
                    double modal = parsePrice(node.path("modal_price").asText("0"));

                    dto.setMinPrice(min);
                    dto.setMaxPrice(max);
                    dto.setModalPrice(modal > 0 ? modal : (min + max) / 2.0);
                    dto.setUnit("₹/quintal");
                    dto.setSource("Government of India / AGMARKNET");
                    dto.setSourceType("GOVERNMENT_API");
                    dto.setSourceLabel("Latest Government Mandi Data");
                    dto.setLastUpdated(dto.getArrivalDate());

                    list.add(dto);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing data.gov.in JSON response: {}", e.getMessage());
        }

        return list;
    }

    private double parsePrice(String raw) {
        if (raw == null) return 0.0;
        try {
            String clean = raw.replaceAll("[^0-9.]", "").trim();
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Fallback to internal database repository.
     */
    private List<MandiPriceDto> fetchFromDatabase(String crop, String state, String district) {
        List<MarketPrice> dbEntities = marketPriceRepository.findAll();
        List<MandiPriceDto> dtoList = new ArrayList<>();

        for (MarketPrice entity : dbEntities) {
            // Filter by crop if provided
            if (crop != null && !entity.getCropName().toLowerCase().contains(crop.toLowerCase())) {
                continue;
            }
            // Filter by state if provided
            if (state != null && !entity.getState().equalsIgnoreCase(state)) {
                continue;
            }
            // Filter by district if provided
            if (district != null && !entity.getDistrict().equalsIgnoreCase(district)) {
                continue;
            }

            MandiPriceDto dto = new MandiPriceDto();
            dto.setId(entity.getId());
            dto.setCommodity(entity.getCropName());
            dto.setVariety(entity.getVariety() != null ? entity.getVariety() : "Standard");
            dto.setMandiName(entity.getMarketName());
            dto.setDistrict(entity.getDistrict());
            dto.setState(entity.getState());
            dto.setMinPrice(entity.getMinPrice());
            dto.setMaxPrice(entity.getMaxPrice());
            dto.setModalPrice(entity.getModalPrice());
            dto.setUnit(entity.getUnit() != null ? entity.getUnit() : "₹/quintal");
            dto.setGrade("FAQ");
            dto.setArrivalDate(entity.getPriceDate() != null ? entity.getPriceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "");
            dto.setSource("Smart Krishi Agricultural Repository");
            dto.setSourceType("ADMIN_DATABASE");
            dto.setSourceLabel("Smart Krishi Admin Data");
            dto.setLastUpdated(dto.getArrivalDate());

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * Resolves genuine coordinates, calculates Haversine distances, applies radius threshold,
     * and constructs Google Maps navigation links.
     */
    private List<MandiPriceDto> processAndFilterRecords(List<MandiPriceDto> rawRecords,
                                                        Double farmerLat,
                                                        Double farmerLon,
                                                        double radiusKm,
                                                        String cropFilter,
                                                        String stateFilter,
                                                        String districtFilter) {
        List<MandiPriceDto> processed = new ArrayList<>();

        for (MandiPriceDto item : rawRecords) {
            // Apply text filters if specified
            if (cropFilter != null && !item.getCommodity().toLowerCase().contains(cropFilter.toLowerCase())) {
                continue;
            }
            if (stateFilter != null && !item.getState().equalsIgnoreCase(stateFilter)) {
                continue;
            }
            if (districtFilter != null && !item.getDistrict().equalsIgnoreCase(districtFilter)) {
                continue;
            }

            // Resolve coordinates
            MandiLocationRegistry.Coordinates coords = mandiLocationRegistry.resolveLocation(
                    item.getMandiName(), item.getDistrict(), item.getState());

            if (coords != null) {
                item.setLatitude(coords.latitude);
                item.setLongitude(coords.longitude);
            }

            // Build Google Maps navigation link
            item.setNavigateUrl(mandiLocationRegistry.buildNavigateUrl(
                    item.getLatitude(), item.getLongitude(), item.getMandiName(), item.getDistrict(), item.getState()));

            // Distance & Radius calculation if farmer coordinates provided
            if (farmerLat != null && farmerLon != null) {
                if (coords != null) {
                    double dist = MandiLocationRegistry.calculateDistance(farmerLat, farmerLon, coords.latitude, coords.longitude);
                    item.setDistanceKm(dist);

                    // Filter out if outside specified radius
                    if (dist > radiusKm) {
                        continue;
                    }
                } else {
                    // Coordinates unknown for this specific market:
                    // If farmer also specified state/district that matches, retain without distance; otherwise skip
                    if (stateFilter != null || districtFilter != null) {
                        item.setDistanceKm(null);
                    } else {
                        // Omit records whose distance cannot be computed when searching strictly by GPS radius
                        continue;
                    }
                }
            } else {
                item.setDistanceKm(null);
            }

            processed.add(item);
        }

        return processed;
    }

    private void sortResults(List<MandiPriceDto> results, String sortOption, boolean hasGpsLocation) {
        if (results == null || results.isEmpty()) return;

        switch (sortOption) {
            case "price_desc":
                results.sort(Comparator.comparing(MandiPriceDto::getModalPrice, Comparator.nullsLast(Double::compareTo)).reversed());
                break;
            case "price_asc":
                results.sort(Comparator.comparing(MandiPriceDto::getModalPrice, Comparator.nullsLast(Double::compareTo)));
                break;
            case "date_desc":
                results.sort(Comparator.comparing(MandiPriceDto::getArrivalDate, Comparator.nullsLast(String::compareTo)).reversed());
                break;
            case "nearest":
            default:
                if (hasGpsLocation) {
                    results.sort(Comparator.comparing(MandiPriceDto::getDistanceKm, Comparator.nullsLast(Double::compareTo)));
                } else {
                    results.sort(Comparator.comparing(MandiPriceDto::getCommodity, String.CASE_INSENSITIVE_ORDER));
                }
                break;
        }
    }

    /**
     * Returns distinct available commodities across the database and government catalogue.
     */
    public List<String> getAvailableCrops() {
        Set<String> crops = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // Common Indian crops
        crops.addAll(Arrays.asList(
                "Tomato", "Potato", "Onion", "Wheat", "Paddy (Rice)",
                "Mustard", "Cotton", "Soybean", "Maize", "Gram (Chana)",
                "Bajra", "Turmeric", "Garlic", "Ginger", "Chilli Green", "Banana"
        ));
        // Also add crops from database
        marketPriceRepository.findAll().forEach(p -> {
            if (p.getCropName() != null && !p.getCropName().isBlank()) {
                crops.add(p.getCropName());
            }
        });
        return new ArrayList<>(crops);
    }

    /**
     * Returns states and their associated districts for dropdown selectors.
     */
    public Map<String, List<String>> getAvailableLocations() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        // Major agricultural states and districts
        map.put("Rajasthan", Arrays.asList("Jaipur", "Kota", "Bikaner", "Bharatpur", "Jodhpur", "Alwar", "Ajmer", "Dausa", "Tonk", "Sikar", "Sri Ganganagar", "Udaipur"));
        map.put("Maharashtra", Arrays.asList("Nashik", "Pune", "Latur", "Nagpur", "Solapur", "Ahmednagar", "Kolhapur", "Mumbai"));
        map.put("Madhya Pradesh", Arrays.asList("Indore", "Ujjain", "Bhopal", "Neemuch", "Mandsaur", "Jabalpur"));
        map.put("Punjab", Arrays.asList("Khanna", "Bathinda", "Ludhiana", "Amritsar", "Jalandhar"));
        map.put("Haryana", Arrays.asList("Karnal", "Sirsa", "Hisar", "Ambala"));
        map.put("Gujarat", Arrays.asList("Rajkot", "Surat", "Ahmedabad", "Patan", "Mehsana"));
        map.put("Uttar Pradesh", Arrays.asList("Agra", "Kanpur", "Lucknow", "Varanasi", "Mathura", "Aligarh"));
        map.put("Karnataka", Arrays.asList("Kolar", "Davangere", "Bengaluru", "Belagavi", "Mysuru"));
        map.put("Telangana", Arrays.asList("Warangal", "Nizamabad", "Hyderabad", "Karimnagar"));
        map.put("Andhra Pradesh", Arrays.asList("Guntur", "Krishna", "Kurnool"));
        map.put("Delhi", Arrays.asList("Delhi"));

        return map;
    }

    /**
     * Returns data source status for administrative transparency.
     */
    public Map<String, Object> getDataSourceStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConfigured = mandiApiKey != null && !mandiApiKey.trim().isBlank();
        status.put("governmentApiConfigured", isConfigured);
        status.put("apiUrl", mandiApiUrl);
        status.put("sourceName", "data.gov.in / AGMARKNET");
        status.put("cacheActiveEntries", apiCache.size());
        status.put("cacheDurationMinutes", cacheDurationMinutes);
        status.put("lastGovApiSync", lastGovApiSync != null ? lastGovApiSync.toString() : "Not synced yet");
        status.put("databaseFallbackRecords", marketPriceRepository.count());
        return status;
    }
}

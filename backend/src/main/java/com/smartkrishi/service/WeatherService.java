package com.smartkrishi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartkrishi.config.WeatherCacheConfig;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.dto.WeatherResponse.ForecastItem;
import com.smartkrishi.dto.WeatherResponse.IrrigationAdvice;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_BASE_URL = "https://api.weatherapi.com/v1";
    private static final long DEFAULT_BACKOFF_MS = 60_000L; // 60s default backoff on 429 / quota
    private static final long MAX_BACKOFF_MS = 300_000L;     // 5m maximum backoff
    private static final int TIMEOUT_FAILURE_THRESHOLD = 2; // Consecutive timeouts to trigger short backoff
    private static final long TIMEOUT_BACKOFF_MS = 20_000L;  // 20s short bounded backoff

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    @Value("${smartkrishi.weatherapi.key:${WEATHERAPI_KEY:}}")
    private String weatherApiKey;

    @Value("${smartkrishi.weatherapi.base-url:https://api.weatherapi.com/v1}")
    private String weatherApiBaseUrl = DEFAULT_BASE_URL;

    private final AtomicLong rateLimitBackoffUntil = new AtomicLong(0L);
    private final AtomicInteger consecutiveTimeouts = new AtomicInteger(0);
    private final AtomicLong timeoutBackoffUntil = new AtomicLong(0L);

    @Autowired
    public WeatherService(RestTemplateBuilder restTemplateBuilder,
                          CacheManager cacheManager,
                          @Value("${smartkrishi.weatherapi.key:${WEATHERAPI_KEY:}}") String weatherApiKey,
                          @Value("${smartkrishi.weatherapi.base-url:https://api.weatherapi.com/v1}") String weatherApiBaseUrl) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(8))
                .build(), cacheManager, weatherApiKey, weatherApiBaseUrl);
    }

    public WeatherService(RestTemplateBuilder restTemplateBuilder,
                          CacheManager cacheManager,
                          String weatherApiKey) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(8))
                .build(), cacheManager, weatherApiKey, DEFAULT_BASE_URL);
    }

    public WeatherService(RestTemplateBuilder restTemplateBuilder, CacheManager cacheManager) {
        this(restTemplateBuilder, cacheManager, Optional.ofNullable(System.getenv("WEATHERAPI_KEY"))
                .filter(k -> !k.isBlank())
                .orElse("test-weatherapi-key"), DEFAULT_BASE_URL);
    }

    public WeatherService(RestTemplate restTemplate, CacheManager cacheManager) {
        this(restTemplate, cacheManager, Optional.ofNullable(System.getenv("WEATHERAPI_KEY"))
                .filter(k -> !k.isBlank())
                .orElse("test-weatherapi-key"), DEFAULT_BASE_URL);
    }

    public WeatherService(RestTemplate restTemplate, CacheManager cacheManager, String weatherApiKey) {
        this(restTemplate, cacheManager, weatherApiKey, DEFAULT_BASE_URL);
    }

    public WeatherService(RestTemplate restTemplate, CacheManager cacheManager, String weatherApiKey, String weatherApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        this.weatherApiKey = (weatherApiKey != null && !weatherApiKey.isBlank()) ? weatherApiKey.trim() : null;
        this.weatherApiBaseUrl = (weatherApiBaseUrl != null && !weatherApiBaseUrl.isBlank()) ? weatherApiBaseUrl.trim() : DEFAULT_BASE_URL;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setWeatherApiKey(String weatherApiKey) {
        this.weatherApiKey = (weatherApiKey != null && !weatherApiKey.isBlank()) ? weatherApiKey.trim() : null;
    }

    public void setApiKey(String apiKey) {
        setWeatherApiKey(apiKey);
    }

    public String getWeatherApiBaseUrl() {
        return weatherApiBaseUrl;
    }

    public void setWeatherApiBaseUrl(String weatherApiBaseUrl) {
        this.weatherApiBaseUrl = (weatherApiBaseUrl != null && !weatherApiBaseUrl.isBlank())
                ? weatherApiBaseUrl.trim()
                : DEFAULT_BASE_URL;
    }

    /**
     * Builds the forecast endpoint URL by appending /forecast.json to the base URL,
     * normalizing any trailing slashes to avoid duplicate slashes.
     */
    public String getForecastEndpointUrl() {
        String base = (weatherApiBaseUrl != null && !weatherApiBaseUrl.isBlank())
                ? weatherApiBaseUrl.trim()
                : DEFAULT_BASE_URL;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/forecast.json";
    }

    /**
     * Safe, normalized cache key for cities (e.g. "  Jaipur  " -> "jaipur").
     */
    public static String normalizeCityKey(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "pune";
        }
        return city.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Safe, normalized cache key for coordinates (rounded to 4 decimals ~11m).
     */
    public static String normalizeCoordsKey(double latitude, double longitude, String customLocationName) {
        String loc = (customLocationName == null) ? "" : customLocationName.trim().toLowerCase(Locale.ROOT);
        return String.format(Locale.US, "%.4f:%.4f:%s", latitude, longitude, loc);
    }

    /**
     * Backward-compatible helper method to fetch weather by city name.
     */
    public WeatherResponse getWeather(String city) {
        return getWeatherByCity(city);
    }

    /**
     * Fetch live real-time weather and 5-day forecast by city/district name using WeatherAPI.com.
     * Cached for 10 minutes in fresh cache. Kept for up to 6 hours in stale fallback cache.
     */
    @Cacheable(cacheNames = WeatherCacheConfig.CACHE_WEATHER_CITY,
            key = "T(com.smartkrishi.service.WeatherService).normalizeCityKey(#city)",
            unless = "#result == null || #result.stale")
    public WeatherResponse getWeatherByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            city = "Pune";
        }
        String cleanCity = city.trim();
        String cityCacheKey = normalizeCityKey(cleanCity);

        // 1. Programmatic fresh cache check in case call bypassed Spring proxy
        WeatherResponse fresh = getFreshWeatherCity(cityCacheKey);
        if (fresh != null && !fresh.isStale()) {
            WeatherResponse cachedResp = fresh.copy();
            cachedResp.setDataSource("CACHED");
            cachedResp.setCached(true);
            return cachedResp;
        }

        // 2. Check if upstream provider is currently in rate-limit/quota backoff
        if (isRateLimited()) {
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("WeatherAPI upstream rate-limited (backoff active). Returning 6-hour stale fallback for city '{}'.", cleanCity);
                return createStaleFallback(stale, "Live weather provider is temporarily rate-limited or quota exceeded. Serving cached telemetry.");
            }
            long waitSec = getRemainingRateLimitSeconds();
            throw new BadRequestException("Live weather is temporarily rate-limited. Please retry in " + waitSec + " seconds.");
        }

        // 2b. Check if upstream provider is currently in network timeout backoff
        if (isTimeoutBackoffActive()) {
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("WeatherAPI upstream timeout backoff active. Returning 6-hour stale fallback for city '{}'.", cleanCity);
                return createStaleFallback(stale, "Live weather service is temporarily experiencing network timeouts. Serving cached telemetry.");
            }
            long waitSec = getRemainingTimeoutSeconds();
            throw new BadRequestException("Live weather service is temporarily experiencing network timeouts. Please retry in " + waitSec + " seconds.");
        }

        // 3. Query WeatherAPI directly using city query
        return queryWeatherApi(cleanCity, null, null, null, cityCacheKey, null);
    }

    /**
     * Fetch live real-time weather and 5-day forecast by GPS coordinates using WeatherAPI.com.
     * WeatherAPI resolves locality, state, and country automatically.
     */
    @Cacheable(cacheNames = WeatherCacheConfig.CACHE_WEATHER_COORDINATES,
            key = "T(com.smartkrishi.service.WeatherService).normalizeCoordsKey(#latitude, #longitude, #customLocationName)",
            unless = "#result == null || #result.stale")
    public WeatherResponse getWeatherByCoordinates(double latitude, double longitude, String customLocationName) {
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Invalid geographical coordinates: Latitude must be between -90 and 90, Longitude between -180 and 180.");
        }

        String coordsKey = normalizeCoordsKey(latitude, longitude, customLocationName);

        // Check fresh cache in case call bypassed Spring proxy
        WeatherResponse fresh = getFreshWeatherCoordinates(coordsKey);
        if (fresh != null && !fresh.isStale()) {
            WeatherResponse cachedResp = fresh.copy();
            cachedResp.setDataSource("CACHED");
            cachedResp.setCached(true);
            return cachedResp;
        }

        // Check if upstream provider is currently in rate-limit/quota backoff
        if (isRateLimited()) {
            WeatherResponse stale = getStaleWeatherCoordinates(coordsKey);
            if (stale != null) {
                logger.info("WeatherAPI upstream rate-limited (backoff active). Returning 6-hour stale fallback for coords '{}'.", coordsKey);
                return createStaleFallback(stale, "Live weather provider is temporarily rate-limited or quota exceeded. Serving cached telemetry.");
            }
            long waitSec = getRemainingRateLimitSeconds();
            throw new BadRequestException("Live weather is temporarily rate-limited. Please retry in " + waitSec + " seconds.");
        }

        // Check if upstream provider is currently in network timeout backoff
        if (isTimeoutBackoffActive()) {
            WeatherResponse stale = getStaleWeatherCoordinates(coordsKey);
            if (stale != null) {
                logger.info("WeatherAPI upstream timeout backoff active. Returning 6-hour stale fallback for coords '{}'.", coordsKey);
                return createStaleFallback(stale, "Live weather service is temporarily experiencing network timeouts. Serving cached telemetry.");
            }
            long waitSec = getRemainingTimeoutSeconds();
            throw new BadRequestException("Live weather service is temporarily experiencing network timeouts. Please retry in " + waitSec + " seconds.");
        }

        return queryWeatherApi(null, latitude, longitude, customLocationName, null, coordsKey);
    }

    private static class WeatherApiError {
        final Integer code;
        final String message;

        WeatherApiError(Integer code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    private WeatherApiError parseWeatherApiError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new WeatherApiError(null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                JsonNode errorNode = root.get("error");
                Integer code = (errorNode.has("code") && errorNode.get("code").isNumber())
                        ? errorNode.get("code").asInt()
                        : null;
                String message = errorNode.has("message")
                        ? errorNode.get("message").asText()
                        : null;
                return new WeatherApiError(code, message);
            }
        } catch (Exception ignored) {
        }
        return new WeatherApiError(null, null);
    }

    /**
     * Executes forecast query against WeatherAPI.com.
     * Supports both city searches ("q=Jaipur") and coordinate queries ("q=26.9124,75.7873").
     */
    @SuppressWarnings("unchecked")
    private WeatherResponse queryWeatherApi(String cityQuery, Double lat, Double lon, String customLocationName,
                                            String cityCacheKey, String coordsCacheKey) {
        String coordsKey = (coordsCacheKey != null)
                ? coordsCacheKey
                : (lat != null && lon != null ? normalizeCoordsKey(lat, lon, customLocationName) : null);

        // Verify API key configuration
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            logger.warn("WEATHERAPI_KEY is not configured. External weather calls cannot proceed.");
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "WEATHERAPI_KEY is not configured. Serving cached telemetry.");
            }
            throw new BadRequestException("WEATHERAPI_KEY environment variable is not configured. Please configure your WeatherAPI.com API key.");
        }

        String queryParam;
        if (cityQuery != null && !cityQuery.isBlank()) {
            queryParam = cityQuery.trim();
        } else if (lat != null && lon != null) {
            queryParam = String.format(Locale.US, "%.4f,%.4f", lat, lon);
        } else {
            queryParam = "Pune";
        }

        String endpoint = getForecastEndpointUrl();
        String url = endpoint
                + "?key=" + URLEncoder.encode(weatherApiKey.trim(), StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(queryParam, StandardCharsets.UTF_8)
                + "&days=5&aqi=no&alerts=no";

        Map<String, Object> body;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(URI.create(url), Map.class);
            body = resp.getBody();
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            WeatherApiError apiError = parseWeatherApiError(responseBody);
            Integer errorCode = apiError.code;
            String errorMessage = apiError.message;

            logger.warn("WeatherAPI HTTP error {} (code: {}) for query '{}'", statusCode, errorCode, queryParam);

            // 1. Precise location not found classification:
            // Must have structured code 1006, or an explicit "no matching location" message
            boolean isLocationNotFound = (errorCode != null && errorCode == 1006)
                    || (errorMessage != null && errorMessage.toLowerCase(Locale.ROOT).contains("no matching location"));

            if (isLocationNotFound) {
                logger.warn("WeatherAPI: No matching location found for query '{}'", queryParam);
                throw new ResourceNotFoundException("Location '" + queryParam + "' not found. Please verify spelling or try another city/district.");
            }

            // 2. Check if Quota Exceeded (WeatherAPI error code 2007) or HTTP 429
            boolean isQuotaOrRateLimit = (statusCode == 429)
                    || (errorCode != null && errorCode == 2007)
                    || (responseBody != null && responseBody.toLowerCase(Locale.ROOT).contains("quota"));

            if (isQuotaOrRateLimit) {
                recordRateLimit(parseRetryAfterMillis(e.getResponseHeaders()));
                WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
                if (stale != null) {
                    logger.info("Returning stale cached weather for '{}' following quota/rate limit error.", queryParam);
                    return createStaleFallback(stale, "Live weather provider quota or rate limit reached. Displaying cached telemetry.");
                }
                throw new BadRequestException("Live weather is temporarily rate-limited or quota exceeded. Please wait a few minutes and try again.");
            }

            // 3. Check if Authentication error (401, 403, error code 2006/2008)
            boolean isAuthError = (statusCode == 401)
                    || (errorCode != null && (errorCode == 2006 || errorCode == 2008))
                    || (statusCode == 403 && !isQuotaOrRateLimit);

            if (isAuthError) {
                WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
                if (stale != null) {
                    logger.info("Returning stale cached weather for '{}' following auth error (HTTP {}).", queryParam, statusCode);
                    return createStaleFallback(stale, "Live weather provider authentication failed. Displaying cached telemetry.");
                }
                throw new BadRequestException("Live weather service authentication failed. Please check WEATHERAPI_KEY configuration.");
            }

            // 4. Other HTTP errors (including other HTTP 400 errors, 5xx, etc.)
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather provider is temporarily unavailable. Displaying cached telemetry.");
            }
            String detail = (errorMessage != null && !errorMessage.isBlank())
                    ? errorMessage
                    : "Live weather provider is temporarily unavailable. Please try again later.";
            throw new BadRequestException("Live weather provider error: " + detail);

        } catch (ResourceAccessException e) {
            logger.error("WeatherAPI network/timeout error for query '{}': {}", queryParam, e.getMessage());
            recordTimeout();
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                logger.info("Returning stale cached weather for '{}' following WeatherAPI network timeout.", queryParam);
                return createStaleFallback(stale, "Live weather service timed out. Displaying cached telemetry.");
            }
            throw new BadRequestException("Live weather service timed out. Please check your network connection.");

        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error querying WeatherAPI for query '{}': {}", queryParam, e.getMessage());
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather provider is temporarily unavailable. Displaying cached telemetry.");
            }
            throw new BadRequestException("Failed to retrieve live weather data: " + e.getMessage());
        }

        if (body == null || !body.containsKey("current") || !body.containsKey("location")) {
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather data currently unavailable from provider. Displaying cached telemetry.");
            }
            throw new BadRequestException("Live weather data currently unavailable from provider.");
        }

        return mapWeatherApiResponse(body, customLocationName, cityCacheKey, coordsKey, queryParam);
    }

    /**
     * Maps WeatherAPI.com JSON response into the existing WeatherResponse structure.
     */
    @SuppressWarnings("unchecked")
    private WeatherResponse mapWeatherApiResponse(Map<String, Object> body, String customLocationName,
                                                  String cityCacheKey, String coordsCacheKey, String originalQuery) {
        Map<String, Object> location = (Map<String, Object>) body.get("location");
        Map<String, Object> current = (Map<String, Object>) body.get("current");
        Map<String, Object> forecast = (Map<String, Object>) body.get("forecast");

        String name = (String) location.get("name");
        String region = (String) location.get("region");
        String country = (String) location.get("country");
        double resolvedLat = location.containsKey("lat") ? ((Number) location.get("lat")).doubleValue() : 0.0;
        double resolvedLon = location.containsKey("lon") ? ((Number) location.get("lon")).doubleValue() : 0.0;
        String tzId = (String) location.get("tz_id");

        String resolvedLocationName;
        if (customLocationName != null && !customLocationName.isBlank()) {
            resolvedLocationName = customLocationName.trim();
        } else if (name != null && !name.isBlank()) {
            if (region != null && !region.isBlank() && !region.equalsIgnoreCase(name)) {
                resolvedLocationName = name + ", " + region;
            } else {
                resolvedLocationName = name;
            }
        } else {
            resolvedLocationName = (originalQuery != null) ? originalQuery : "Jaipur, Rajasthan";
        }

        double temp = current.containsKey("temp_c") ? ((Number) current.get("temp_c")).doubleValue() : 0.0;
        double feelsLike = current.containsKey("feelslike_c") ? ((Number) current.get("feelslike_c")).doubleValue() : temp;
        int humidity = current.containsKey("humidity") ? ((Number) current.get("humidity")).intValue() : 0;
        double precip = current.containsKey("precip_mm") ? ((Number) current.get("precip_mm")).doubleValue() : 0.0;
        double windKph = current.containsKey("wind_kph") ? ((Number) current.get("wind_kph")).doubleValue() : 0.0;
        double windDegree = current.containsKey("wind_degree") ? ((Number) current.get("wind_degree")).doubleValue() : 0.0;
        int cloud = current.containsKey("cloud") ? ((Number) current.get("cloud")).intValue() : 0;
        boolean isDay = !current.containsKey("is_day") || ((Number) current.get("is_day")).intValue() == 1;

        Map<String, Object> conditionMap = (Map<String, Object>) current.get("condition");
        String conditionText = conditionMap != null && conditionMap.containsKey("text") ? (String) conditionMap.get("text") : "Clear";
        int conditionCode = conditionMap != null && conditionMap.containsKey("code") ? ((Number) conditionMap.get("code")).intValue() : 1000;
        String conditionIcon = mapWeatherApiConditionToIcon(conditionCode, isDay);

        String rawTime = (String) current.get("last_updated");
        String formattedUpdatedTime = formatObservationTime(rawTime);

        WeatherResponse resp = new WeatherResponse();
        resp.setLocation(resolvedLocationName);
        resp.setCountry(country != null && !country.isBlank() ? country : "India");
        resp.setLatitude(resolvedLat);
        resp.setLongitude(resolvedLon);
        resp.setTimezone(tzId);
        resp.setTemperature(temp);
        resp.setFeelsLike(feelsLike);
        resp.setApparentTemperature(feelsLike);
        resp.setHumidity(humidity);
        resp.setPrecipitation(precip);
        resp.setRainfall(precip);
        resp.setWindSpeed(windKph);
        resp.setWindDirection(windDegree);
        resp.setCloudCover(cloud);
        resp.setCondition(conditionText);
        resp.setConditionIcon(conditionIcon);
        resp.setLastUpdated(formattedUpdatedTime);

        // Reliability metadata
        resp.setDataSource("LIVE");
        resp.setCached(false);
        resp.setStale(false);
        resp.setCachedAt(System.currentTimeMillis());
        resp.setNotice(null);

        // Parse Forecast items (up to 5 days)
        List<ForecastItem> forecastList = new ArrayList<>();
        if (forecast != null && forecast.containsKey("forecastday")) {
            List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
            int count = Math.min(forecastDays.size(), 5);
            for (int i = 0; i < count; i++) {
                Map<String, Object> fDay = forecastDays.get(i);
                String dateStr = (String) fDay.get("date");
                String dayOfWeek = "Today";
                try {
                    if (dateStr != null) {
                        LocalDate ld = LocalDate.parse(dateStr);
                        dayOfWeek = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    }
                } catch (Exception ignored) {}

                Map<String, Object> day = (Map<String, Object>) fDay.get("day");
                double tMin = (day != null && day.containsKey("mintemp_c")) ? ((Number) day.get("mintemp_c")).doubleValue() : temp;
                double tMax = (day != null && day.containsKey("maxtemp_c")) ? ((Number) day.get("maxtemp_c")).doubleValue() : temp;
                Map<String, Object> dayCond = (day != null) ? (Map<String, Object>) day.get("condition") : null;
                String dayCondText = (dayCond != null && dayCond.containsKey("text")) ? (String) dayCond.get("text") : "Clear";
                int dayCondCode = (dayCond != null && dayCond.containsKey("code")) ? ((Number) dayCond.get("code")).intValue() : 1000;
                String dayCondIcon = mapWeatherApiConditionToIcon(dayCondCode, true);
                double rainProb = (day != null && day.containsKey("daily_chance_of_rain")) ? ((Number) day.get("daily_chance_of_rain")).doubleValue() : 0.0;
                double rainMm = (day != null && day.containsKey("totalprecip_mm")) ? ((Number) day.get("totalprecip_mm")).doubleValue() : 0.0;

                forecastList.add(new ForecastItem(
                        dateStr,
                        dayOfWeek,
                        tMin,
                        tMax,
                        dayCondText,
                        dayCondIcon,
                        rainProb,
                        rainMm
                ));
            }
        }
        resp.setForecast(forecastList);

        // Precision irrigation advice from live telemetry
        resp.setIrrigationAdvice(calculateIrrigationAdvice(temp, humidity, precip, "Alluvial"));

        // Reset consecutive timeout counter upon successful upstream response
        recordSuccess();

        // Populate fresh and 6-hour stale fallback caches
        String cKey = (coordsCacheKey != null) ? coordsCacheKey : normalizeCoordsKey(resolvedLat, resolvedLon, customLocationName);
        putInCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES, cKey, resp);
        putInCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES_STALE, cKey, resp);

        String cCityKey = (cityCacheKey != null) ? cityCacheKey : (name != null ? normalizeCityKey(name) : null);
        if (cCityKey != null && !cCityKey.isBlank()) {
            putInCache(WeatherCacheConfig.CACHE_WEATHER_CITY, cCityKey, resp);
            putInCache(WeatherCacheConfig.CACHE_WEATHER_CITY_STALE, cCityKey, resp);
        }

        return resp;
    }

    private WeatherResponse findStaleFallback(String cityCacheKey, String coordsKey) {
        if (cityCacheKey != null) {
            WeatherResponse byCity = getStaleWeatherCity(cityCacheKey);
            if (byCity != null) return byCity;
        }
        if (coordsKey != null) {
            return getStaleWeatherCoordinates(coordsKey);
        }
        return null;
    }

    private WeatherResponse getStaleWeatherCity(String key) {
        return getFromCache(WeatherCacheConfig.CACHE_WEATHER_CITY_STALE, key);
    }

    private WeatherResponse getStaleWeatherCoordinates(String key) {
        return getFromCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES_STALE, key);
    }

    private WeatherResponse getFreshWeatherCity(String key) {
        return getFromCache(WeatherCacheConfig.CACHE_WEATHER_CITY, key);
    }

    private WeatherResponse getFreshWeatherCoordinates(String key) {
        return getFromCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES, key);
    }

    private WeatherResponse getFromCache(String cacheName, String key) {
        if (cacheManager == null || key == null) return null;
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                return cache.get(key, WeatherResponse.class);
            }
        } catch (Exception e) {
            logger.debug("Failed to read from cache '{}' with key '{}': {}", cacheName, key, e.getMessage());
        }
        return null;
    }

    private void putInCache(String cacheName, String key, WeatherResponse resp) {
        if (cacheManager == null || key == null || resp == null) return;
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, resp.copy());
            }
        } catch (Exception e) {
            logger.debug("Failed to write to cache '{}' with key '{}': {}", cacheName, key, e.getMessage());
        }
    }

    private WeatherResponse createStaleFallback(WeatherResponse original, String notice) {
        if (original == null) return null;
        WeatherResponse fallback = original.copy();
        fallback.setDataSource("STALE_FALLBACK");
        fallback.setCached(true);
        fallback.setStale(true);
        fallback.setNotice(notice);
        return fallback;
    }

    public boolean isRateLimited() {
        return System.currentTimeMillis() < rateLimitBackoffUntil.get();
    }

    public long getRemainingRateLimitSeconds() {
        long diff = rateLimitBackoffUntil.get() - System.currentTimeMillis();
        return Math.max(1L, (diff + 999L) / 1000L);
    }

    public void recordRateLimit(long retryAfterMs) {
        long backoff = Math.min(Math.max(retryAfterMs, 5000L), MAX_BACKOFF_MS);
        long target = System.currentTimeMillis() + backoff;
        rateLimitBackoffUntil.updateAndGet(current -> Math.max(current, target));
        logger.warn("WeatherAPI rate limit recorded. Backing off external calls for {} ms (until {})", backoff, target);
    }

    public void clearRateLimit() {
        rateLimitBackoffUntil.set(0L);
    }

    public boolean isTimeoutBackoffActive() {
        return System.currentTimeMillis() < timeoutBackoffUntil.get();
    }

    public long getRemainingTimeoutSeconds() {
        long diff = timeoutBackoffUntil.get() - System.currentTimeMillis();
        return Math.max(1L, (diff + 999L) / 1000L);
    }

    public void recordTimeout() {
        int count = consecutiveTimeouts.incrementAndGet();
        if (count >= TIMEOUT_FAILURE_THRESHOLD) {
            long target = System.currentTimeMillis() + TIMEOUT_BACKOFF_MS;
            timeoutBackoffUntil.updateAndGet(current -> Math.max(current, target));
            logger.warn("Repeated WeatherAPI timeouts observed (consecutive={}). Backing off external calls for {} ms (until {})",
                    count, TIMEOUT_BACKOFF_MS, target);
        }
    }

    public void recordSuccess() {
        consecutiveTimeouts.set(0);
        timeoutBackoffUntil.set(0L);
    }

    public void clearTimeoutBackoff() {
        consecutiveTimeouts.set(0);
        timeoutBackoffUntil.set(0L);
    }

    public int getConsecutiveTimeouts() {
        return consecutiveTimeouts.get();
    }

    private long parseRetryAfterMillis(HttpHeaders headers) {
        if (headers == null) {
            return DEFAULT_BACKOFF_MS;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return DEFAULT_BACKOFF_MS;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return Math.max(seconds * 1000L, 5000L);
        } catch (NumberFormatException e) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                long diff = ChronoUnit.MILLIS.between(Instant.now(), zdt.toInstant());
                if (diff > 0) {
                    return Math.max(diff, 5000L);
                }
            } catch (Exception ignored) {
            }
        }
        return DEFAULT_BACKOFF_MS;
    }

    private String formatObservationTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(rawTime.trim(), formatter);
            return ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        } catch (Exception e1) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(rawTime.trim());
                return ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            } catch (Exception e2) {
                return rawTime;
            }
        }
    }

    public IrrigationAdvice calculateIrrigationAdvice(double temp, int humidity, double rainfallMm, String soilType) {
        if (rainfallMm >= 5.0) {
            return new IrrigationAdvice(
                    "NO_IRRIGATION",
                    "success",
                    "No Irrigation Required Today",
                    "Live weather recorded " + String.format(Locale.US, "%.1f", rainfallMm) + " mm of precipitation. Soil profile has sufficient water content. Avoid overwatering to prevent root asphyxiation.",
                    "Pause Irrigation",
                    "High (Saturated / Field Capacity)"
            );
        } else if (temp >= 33.0 && humidity < 45) {
            return new IrrigationAdvice(
                    "HEAVY_IRRIGATION",
                    "danger",
                    "High Evaporative Stress – Deep Irrigation Needed",
                    "Elevated temperature (" + String.format(Locale.US, "%.1f", temp) + "°C) and dry air (" + humidity + "% humidity) indicate high crop evapotranspiration. Water deeply to reach the active root zone.",
                    "Early Morning (5:30 AM – 8:00 AM) or Evening (6:00 PM – 8:00 PM)",
                    "Low (Depleted topsoil moisture)"
            );
        } else if (temp >= 24.0 && humidity < 75) {
            return new IrrigationAdvice(
                    "MODERATE_IRRIGATION",
                    "warning",
                    "Moderate Irrigation Recommended",
                    "Standard climatic conditions with " + String.format(Locale.US, "%.1f", temp) + "°C and " + humidity + "% humidity. Apply scheduled drip or furrow irrigation to sustain vegetative growth.",
                    "Morning (6:30 AM – 9:00 AM)",
                    "Moderate (Standard field capacity)"
            );
        } else {
            return new IrrigationAdvice(
                    "LIGHT_IRRIGATION",
                    "info",
                    "Light Irrigation / Moisture Maintenance",
                    "Mild conditions observed (" + String.format(Locale.US, "%.1f", temp) + "°C, " + humidity + "% humidity). Light watering cycle is adequate to sustain root vitality.",
                    "Morning (7:00 AM – 9:30 AM)",
                    "Adequate (Good retention)"
            );
        }
    }

    /**
     * Maps WeatherAPI condition codes to standard Bootstrap Icons.
     */
    public static String mapWeatherApiConditionToIcon(int code, boolean isDay) {
        if (code == 1000) { // Sunny / Clear
            return isDay ? "bi-sun-fill" : "bi-moon-stars-fill";
        }
        if (code == 1003) { // Partly cloudy
            return isDay ? "bi-cloud-sun-fill" : "bi-cloud-moon-fill";
        }
        if (code == 1006 || code == 1009) { // Cloudy / Overcast
            return "bi-clouds-fill";
        }
        if (code == 1030 || code == 1135 || code == 1147) { // Mist, Fog
            return "bi-cloud-fog2-fill";
        }
        if (code == 1063 || code == 1150 || code == 1153 || code == 1168 || code == 1171) { // Drizzle
            return "bi-cloud-drizzle-fill";
        }
        if (code == 1066 || code == 1114 || code == 1117 || code >= 1210 && code <= 1225) { // Snow
            return "bi-snow";
        }
        if (code == 1069 || code == 1072 || code == 1198 || code == 1201 || code >= 1204 && code <= 1207 || code >= 1249 && code <= 1264) { // Sleet / Freezing rain
            return "bi-cloud-sleet-fill";
        }
        if (code == 1087) { // Thundery
            return "bi-cloud-lightning-fill";
        }
        if (code == 1180 || code == 1183 || code == 1186 || code == 1189) { // Light / Moderate rain
            return "bi-cloud-rain-fill";
        }
        if (code == 1192 || code == 1195 || code >= 1240 && code <= 1246) { // Heavy rain / showers
            return "bi-cloud-rain-heavy-fill";
        }
        if (code >= 1273 && code <= 1282) { // Thunder with rain or snow
            return "bi-cloud-lightning-rain-fill";
        }
        return isDay ? "bi-sun-fill" : "bi-moon-stars-fill";
    }
}

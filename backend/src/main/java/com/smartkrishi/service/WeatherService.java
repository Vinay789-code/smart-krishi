package com.smartkrishi.service;

import com.smartkrishi.config.WeatherCacheConfig;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.dto.WeatherResponse.ForecastItem;
import com.smartkrishi.dto.WeatherResponse.IrrigationAdvice;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final long DEFAULT_BACKOFF_MS = 60_000L; // 60s default backoff on 429
    private static final long MAX_BACKOFF_MS = 300_000L;     // 5m maximum backoff
    private static final int TIMEOUT_FAILURE_THRESHOLD = 2; // Consecutive timeouts to trigger short backoff
    private static final long TIMEOUT_BACKOFF_MS = 20_000L;  // 20s short bounded backoff

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final AtomicLong rateLimitBackoffUntil = new AtomicLong(0L);
    private final AtomicInteger consecutiveTimeouts = new AtomicInteger(0);
    private final AtomicLong timeoutBackoffUntil = new AtomicLong(0L);

    @Autowired
    public WeatherService(RestTemplateBuilder restTemplateBuilder, CacheManager cacheManager) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(8))
                .build(), cacheManager);
    }

    public WeatherService(RestTemplate restTemplate, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
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
     * Fetch live real-time weather and 5-day forecast by city/district name.
     * Uses Open-Meteo Geocoding API to resolve exact coordinates.
     * Cached for 10 minutes in fresh cache. Kept for up to 6 hours in stale fallback cache.
     */
    @Cacheable(cacheNames = WeatherCacheConfig.CACHE_WEATHER_CITY,
            key = "T(com.smartkrishi.service.WeatherService).normalizeCityKey(#city)",
            unless = "#result == null || #result.stale")
    @SuppressWarnings("unchecked")
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

        // 2. Check if upstream provider is currently in a 429 rate-limit backoff window
        if (isRateLimited()) {
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("Open-Meteo upstream rate-limited (backoff active). Returning 6-hour stale fallback for city '{}'.", cleanCity);
                return createStaleFallback(stale, "Live weather provider is temporarily rate-limited (HTTP 429). Serving cached telemetry.");
            }
            long waitSec = getRemainingRateLimitSeconds();
            throw new BadRequestException("Live weather is temporarily rate-limited. Please retry in " + waitSec + " seconds.");
        }

        // 2b. Check if upstream provider is currently in a network timeout backoff window
        if (isTimeoutBackoffActive()) {
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("Open-Meteo upstream timeout backoff active. Returning 6-hour stale fallback for city '{}'.", cleanCity);
                return createStaleFallback(stale, "Live weather service is temporarily experiencing network timeouts. Serving cached telemetry.");
            }
            long waitSec = getRemainingTimeoutSeconds();
            throw new BadRequestException("Live weather service is temporarily experiencing network timeouts. Please retry in " + waitSec + " seconds.");
        }

        // 3. Resolve coordinates using Open-Meteo Geocoding API
        double lat;
        double lon;
        String resolvedLocationName;
        String country = "India";

        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(cleanCity, StandardCharsets.UTF_8)
                + "&count=1&language=en&format=json";

        try {
            ResponseEntity<Map> geoResp = restTemplate.getForEntity(geoUrl, Map.class);
            Map<String, Object> geoBody = geoResp.getBody();

            if (geoBody == null || !geoBody.containsKey("results")) {
                logger.warn("Open-Meteo geocoding found no results for '{}'", cleanCity);
                throw new ResourceNotFoundException("Location '" + cleanCity + "' not found. Please verify spelling or try another city/district.");
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) geoBody.get("results");
            if (results == null || results.isEmpty()) {
                throw new ResourceNotFoundException("Location '" + cleanCity + "' not found. Please verify spelling or try another city/district.");
            }

            Map<String, Object> first = results.get(0);
            lat = ((Number) first.get("latitude")).doubleValue();
            lon = ((Number) first.get("longitude")).doubleValue();
            String name = (String) first.get("name");
            String admin1 = (String) first.get("admin1"); // State / province
            String countryName = (String) first.get("country");
            if (countryName != null && !countryName.isBlank()) {
                country = countryName;
            }

            if (admin1 != null && !admin1.isBlank() && !admin1.equalsIgnoreCase(name)) {
                resolvedLocationName = name + ", " + admin1;
            } else {
                resolvedLocationName = name;
            }
        } catch (ResourceNotFoundException e) {
            throw e; // Never return fake weather for an invalid/non-existent location
        } catch (RestClientResponseException e) {
            logger.warn("Open-Meteo Geocoding API HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 429) {
                recordRateLimit(parseRetryAfterMillis(e.getResponseHeaders()));
            }
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("Returning stale cached weather for city '{}' following geocoding HTTP {} error.", cleanCity, e.getStatusCode());
                String reason = (e.getStatusCode().value() == 429)
                        ? "Live weather provider is temporarily rate-limited (HTTP 429). Serving cached telemetry."
                        : "Live weather provider is temporarily unavailable. Serving cached telemetry.";
                return createStaleFallback(stale, reason);
            }
            if (e.getStatusCode().value() == 429) {
                throw new BadRequestException("Live weather is temporarily rate-limited. Please wait a few minutes and try again.");
            }
            throw new BadRequestException("Live weather service temporarily unavailable (Geocoding error). Please try again.");
        } catch (ResourceAccessException e) {
            logger.error("Open-Meteo Geocoding API timeout/connectivity error: {}", e.getMessage());
            recordTimeout();
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                logger.info("Returning stale cached weather for city '{}' following geocoding timeout.", cleanCity);
                return createStaleFallback(stale, "Live weather service network timeout. Serving cached telemetry.");
            }
            throw new BadRequestException("Live weather service network timeout. Please check internet connection.");
        } catch (Exception e) {
            logger.error("Unexpected error during geocoding: {}", e.getMessage());
            WeatherResponse stale = getStaleWeatherCity(cityCacheKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather provider is temporarily unavailable. Serving cached telemetry.");
            }
            throw new BadRequestException("Failed to resolve location '" + cleanCity + "': " + e.getMessage());
        }

        // 4. Query live weather and 5-day forecast using resolved coordinates
        return fetchLiveWeather(lat, lon, resolvedLocationName, country, cityCacheKey, null);
    }

    /**
     * Fetch live real-time weather and 5-day forecast by GPS coordinates.
     * Uses reverse geocoding to resolve a human-readable location name.
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

        // Check if upstream provider is currently in rate-limit backoff window
        if (isRateLimited()) {
            WeatherResponse stale = getStaleWeatherCoordinates(coordsKey);
            if (stale != null) {
                logger.info("Open-Meteo upstream rate-limited (backoff active). Returning 6-hour stale fallback for coords '{}'.", coordsKey);
                return createStaleFallback(stale, "Live weather provider is temporarily rate-limited (HTTP 429). Serving cached telemetry.");
            }
            long waitSec = getRemainingRateLimitSeconds();
            throw new BadRequestException("Live weather is temporarily rate-limited. Please retry in " + waitSec + " seconds.");
        }

        // Check if upstream provider is currently in network timeout backoff window
        if (isTimeoutBackoffActive()) {
            WeatherResponse stale = getStaleWeatherCoordinates(coordsKey);
            if (stale != null) {
                logger.info("Open-Meteo upstream timeout backoff active. Returning 6-hour stale fallback for coords '{}'.", coordsKey);
                return createStaleFallback(stale, "Live weather service is temporarily experiencing network timeouts. Serving cached telemetry.");
            }
            long waitSec = getRemainingTimeoutSeconds();
            throw new BadRequestException("Live weather service is temporarily experiencing network timeouts. Please retry in " + waitSec + " seconds.");
        }

        String resolvedName = customLocationName;
        String country = "India";

        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = reverseGeocode(latitude, longitude);
        }

        return fetchLiveWeather(latitude, longitude, resolvedName, country, null, coordsKey);
    }

    /**
     * Reverse geocode coordinates to a locality name using free BigDataCloud API with fallback.
     */
    @SuppressWarnings("unchecked")
    private String reverseGeocode(double lat, double lon) {
        try {
            String reverseUrl = String.format(Locale.US,
                    "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.4f&longitude=%.4f&localityLanguage=en",
                    lat, lon);
            ResponseEntity<Map> resp = restTemplate.getForEntity(reverseUrl, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body != null) {
                String city = (String) body.get("city");
                if (city == null || city.isBlank()) {
                    city = (String) body.get("locality");
                }
                String state = (String) body.get("principalSubdivision");
                if (city != null && !city.isBlank()) {
                    return (state != null && !state.isBlank() && !state.equalsIgnoreCase(city))
                            ? city + ", " + state
                            : city;
                }
            }
        } catch (Exception e) {
            logger.debug("Reverse geocode lookup skipped for ({}, {}): {}", lat, lon, e.getMessage());
        }
        return String.format(Locale.US, "Location (%.2f° N, %.2f° E)", lat, lon);
    }

    /**
     * Calls Open-Meteo Weather API to retrieve actual live temperature, atmospheric metrics, and 5-day forecast.
     */
    @SuppressWarnings("unchecked")
    private WeatherResponse fetchLiveWeather(double lat, double lon, String locationName, String country, String cityCacheKey, String coordsCacheKey) {
        String coordsKey = (coordsCacheKey != null) ? coordsCacheKey : normalizeCoordsKey(lat, lon, null);
        String forecastUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max&timezone=auto",
                lat, lon);

        Map<String, Object> body;
        try {
            ResponseEntity<Map> weatherResp = restTemplate.getForEntity(forecastUrl, Map.class);
            body = weatherResp.getBody();
        } catch (RestClientResponseException e) {
            logger.warn("Open-Meteo Weather API HTTP error: {} - {}", e.getStatusCode(), e.getMessage());

            if (e.getStatusCode().value() == 429) {
                recordRateLimit(parseRetryAfterMillis(e.getResponseHeaders()));
            }

            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                logger.info("Returning stale cached weather for '{}' following Open-Meteo HTTP {} error.", locationName, e.getStatusCode());
                String reason = (e.getStatusCode().value() == 429)
                        ? "Live weather is temporarily rate-limited (HTTP 429). Displaying cached telemetry."
                        : "Live weather provider is temporarily unavailable. Displaying cached telemetry.";
                return createStaleFallback(stale, reason);
            }

            if (e.getStatusCode().value() == 429) {
                throw new BadRequestException(
                        "Live weather is temporarily rate-limited. Please wait a few minutes and try again.");
            }

            throw new BadRequestException(
                    "Live weather provider is temporarily unavailable. Please try again later.");
        } catch (ResourceAccessException e) {
            logger.error("Open-Meteo Weather API timeout/network error: {}", e.getMessage());
            recordTimeout();
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                logger.info("Returning stale cached weather for '{}' following Open-Meteo network timeout.", locationName);
                return createStaleFallback(stale, "Live weather service timed out. Displaying cached telemetry.");
            }
            throw new BadRequestException("Live weather service timed out. Please check your network connection.");
        } catch (Exception e) {
            logger.error("Failed to query Open-Meteo weather: {}", e.getMessage());
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather provider is temporarily unavailable. Displaying cached telemetry.");
            }
            throw new BadRequestException("Unable to retrieve live weather data: " + e.getMessage());
        }

        if (body == null || !body.containsKey("current")) {
            WeatherResponse stale = findStaleFallback(cityCacheKey, coordsKey);
            if (stale != null) {
                return createStaleFallback(stale, "Live weather data currently unavailable from provider. Displaying cached telemetry.");
            }
            throw new BadRequestException("Live weather data currently unavailable from provider.");
        }

        Map<String, Object> current = (Map<String, Object>) body.get("current");
        Map<String, Object> daily = (Map<String, Object>) body.get("daily");

        WeatherResponse resp = new WeatherResponse();
        resp.setLocation(locationName);
        resp.setCountry(country);
        resp.setLatitude(lat);
        resp.setLongitude(lon);
        resp.setTimezone((String) body.get("timezone"));

        double temp = ((Number) current.get("temperature_2m")).doubleValue();
        double feelsLike = current.containsKey("apparent_temperature")
                ? ((Number) current.get("apparent_temperature")).doubleValue()
                : temp;
        int humidity = ((Number) current.get("relative_humidity_2m")).intValue();
        double precipitation = current.containsKey("precipitation")
                ? ((Number) current.get("precipitation")).doubleValue()
                : 0.0;
        double rain = current.containsKey("rain")
                ? ((Number) current.get("rain")).doubleValue()
                : precipitation;
        double wind = ((Number) current.get("wind_speed_10m")).doubleValue();
        double windDir = current.containsKey("wind_direction_10m")
                ? ((Number) current.get("wind_direction_10m")).doubleValue()
                : 0.0;
        int cloudCover = current.containsKey("cloud_cover")
                ? ((Number) current.get("cloud_cover")).intValue()
                : 0;
        int weatherCode = ((Number) current.get("weather_code")).intValue();

        String rawTime = (String) current.get("time");
        String formattedUpdatedTime = formatObservationTime(rawTime);

        resp.setTemperature(temp);
        resp.setFeelsLike(feelsLike);
        resp.setApparentTemperature(feelsLike);
        resp.setHumidity(humidity);
        resp.setPrecipitation(precipitation);
        resp.setRainfall(rain);
        resp.setWindSpeed(wind);
        resp.setWindDirection(windDir);
        resp.setCloudCover(cloudCover);
        resp.setCondition(mapWmoCodeToCondition(weatherCode));
        resp.setConditionIcon(mapWmoCodeToIcon(weatherCode));
        resp.setLastUpdated(formattedUpdatedTime);

        // Reliability metadata
        resp.setDataSource("LIVE");
        resp.setCached(false);
        resp.setStale(false);
        resp.setCachedAt(System.currentTimeMillis());
        resp.setNotice(null);

        // Parse daily 5-day forecast
        List<ForecastItem> forecastList = new ArrayList<>();
        if (daily != null) {
            List<String> dates = (List<String>) daily.get("time");
            List<Number> maxTemps = (List<Number>) daily.get("temperature_2m_max");
            List<Number> minTemps = (List<Number>) daily.get("temperature_2m_min");
            List<Number> codes = (List<Number>) daily.get("weather_code");
            List<Number> rainSums = (List<Number>) daily.get("precipitation_sum");
            List<Number> rainProbs = (List<Number>) daily.get("precipitation_probability_max");

            int count = Math.min(dates != null ? dates.size() : 0, 5);
            for (int i = 0; i < count; i++) {
                LocalDate date = LocalDate.parse(dates.get(i));
                String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                double tMin = minTemps.get(i).doubleValue();
                double tMax = maxTemps.get(i).doubleValue();
                int wCode = codes.get(i).intValue();
                double rainSum = (rainSums != null && i < rainSums.size() && rainSums.get(i) != null)
                        ? rainSums.get(i).doubleValue()
                        : 0.0;
                double rainProb = (rainProbs != null && i < rainProbs.size() && rainProbs.get(i) != null)
                        ? rainProbs.get(i).doubleValue()
                        : 0.0;

                forecastList.add(new ForecastItem(
                        dates.get(i),
                        dayName,
                        tMin,
                        tMax,
                        mapWmoCodeToCondition(wCode),
                        mapWmoCodeToIcon(wCode),
                        rainProb,
                        rainSum
                ));
            }
        }
        resp.setForecast(forecastList);

        // Calculate precision irrigation advice from real telemetry
        resp.setIrrigationAdvice(calculateIrrigationAdvice(temp, humidity, rain, "Alluvial"));

        // Reset consecutive timeout counter upon successful upstream response
        recordSuccess();

        // Store into fresh caches and 6-hour stale fallback caches
        putInCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES, coordsKey, resp);
        putInCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES_STALE, coordsKey, resp);
        if (cityCacheKey != null && !cityCacheKey.isBlank()) {
            putInCache(WeatherCacheConfig.CACHE_WEATHER_CITY, cityCacheKey, resp);
            putInCache(WeatherCacheConfig.CACHE_WEATHER_CITY_STALE, cityCacheKey, resp);
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
        logger.warn("Open-Meteo rate limit recorded. Backing off external calls for {} ms (until {})", backoff, target);
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
            logger.warn("Repeated Open-Meteo timeouts observed (consecutive={}). Backing off external calls for {} ms (until {})",
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

    private String formatObservationTime(String rawIsoTime) {
        if (rawIsoTime == null || rawIsoTime.isBlank()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(rawIsoTime);
            return ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        } catch (Exception e) {
            return rawIsoTime;
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

    private String mapWmoCodeToCondition(int code) {
        if (code == 0) return "Clear Sky";
        if (code == 1) return "Mainly Clear";
        if (code == 2) return "Partly Cloudy";
        if (code == 3) return "Overcast";
        if (code >= 45 && code <= 48) return "Foggy / Mist";
        if (code >= 51 && code <= 55) return "Light Drizzle";
        if (code >= 61 && code <= 65) return "Rain Showers";
        if (code >= 71 && code <= 77) return "Snow Flurries";
        if (code >= 80 && code <= 82) return "Heavy Rain Showers";
        if (code >= 95) return "Thunderstorm";
        return "Pleasant / Clear";
    }

    private String mapWmoCodeToIcon(int code) {
        if (code == 0) return "bi-sun-fill";
        if (code == 1 || code == 2) return "bi-cloud-sun-fill";
        if (code == 3) return "bi-clouds-fill";
        if (code >= 45 && code <= 48) return "bi-cloud-fog2-fill";
        if (code >= 51 && code <= 55) return "bi-cloud-drizzle-fill";
        if (code >= 61 && code <= 65) return "bi-cloud-rain-fill";
        if (code >= 80 && code <= 82) return "bi-cloud-rain-heavy-fill";
        if (code >= 95) return "bi-cloud-lightning-rain-fill";
        return "bi-sun-fill";
    }
}

package com.smartkrishi.service;

import com.smartkrishi.config.WeatherCacheConfig;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WeatherServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private CacheManager cacheManager;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        cacheManager = new WeatherCacheConfig().cacheManager();
        weatherService = new WeatherService(restTemplate, cacheManager, "test-api-key");
        weatherService.clearRateLimit();
        weatherService.clearTimeoutBackoff();
    }

    private String weatherApiJson(String name, String region, double lat, double lon,
                                  double temp, double feelsLike, int humidity, double rain,
                                  double windKph, double windDegree, int cloud,
                                  String conditionText, int conditionCode, boolean isDay) {
        return "{\n" +
                "  \"location\": {\n" +
                "    \"name\": \"" + name + "\",\n" +
                "    \"region\": \"" + region + "\",\n" +
                "    \"country\": \"India\",\n" +
                "    \"lat\": " + lat + ",\n" +
                "    \"lon\": " + lon + ",\n" +
                "    \"tz_id\": \"Asia/Kolkata\",\n" +
                "    \"localtime\": \"2026-10-10 12:00\"\n" +
                "  },\n" +
                "  \"current\": {\n" +
                "    \"last_updated\": \"2026-10-10 12:00\",\n" +
                "    \"temp_c\": " + temp + ",\n" +
                "    \"feelslike_c\": " + feelsLike + ",\n" +
                "    \"humidity\": " + humidity + ",\n" +
                "    \"precip_mm\": " + rain + ",\n" +
                "    \"wind_kph\": " + windKph + ",\n" +
                "    \"wind_degree\": " + windDegree + ",\n" +
                "    \"cloud\": " + cloud + ",\n" +
                "    \"is_day\": " + (isDay ? 1 : 0) + ",\n" +
                "    \"condition\": {\n" +
                "      \"text\": \"" + conditionText + "\",\n" +
                "      \"icon\": \"//cdn.weatherapi.com/weather/64x64/day/116.png\",\n" +
                "      \"code\": " + conditionCode + "\n" +
                "    }\n" +
                "  },\n" +
                "  \"forecast\": {\n" +
                "    \"forecastday\": [\n" +
                "      {\n" +
                "        \"date\": \"2026-10-10\",\n" +
                "        \"day\": {\n" +
                "          \"maxtemp_c\": 34.0,\n" +
                "          \"mintemp_c\": 22.0,\n" +
                "          \"totalprecip_mm\": 0.0,\n" +
                "          \"daily_chance_of_rain\": 10,\n" +
                "          \"condition\": {\"text\": \"Sunny\", \"code\": 1000}\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"date\": \"2026-10-11\",\n" +
                "        \"day\": {\n" +
                "          \"maxtemp_c\": 33.0,\n" +
                "          \"mintemp_c\": 21.0,\n" +
                "          \"totalprecip_mm\": 0.0,\n" +
                "          \"daily_chance_of_rain\": 5,\n" +
                "          \"condition\": {\"text\": \"Sunny\", \"code\": 1000}\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"date\": \"2026-10-12\",\n" +
                "        \"day\": {\n" +
                "          \"maxtemp_c\": 32.0,\n" +
                "          \"mintemp_c\": 20.0,\n" +
                "          \"totalprecip_mm\": 0.5,\n" +
                "          \"daily_chance_of_rain\": 30,\n" +
                "          \"condition\": {\"text\": \"Patchy rain possible\", \"code\": 1063}\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"date\": \"2026-10-13\",\n" +
                "        \"day\": {\n" +
                "          \"maxtemp_c\": 31.0,\n" +
                "          \"mintemp_c\": 19.0,\n" +
                "          \"totalprecip_mm\": 4.2,\n" +
                "          \"daily_chance_of_rain\": 70,\n" +
                "          \"condition\": {\"text\": \"Moderate rain\", \"code\": 1189}\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"date\": \"2026-10-14\",\n" +
                "        \"day\": {\n" +
                "          \"maxtemp_c\": 30.0,\n" +
                "          \"mintemp_c\": 18.0,\n" +
                "          \"totalprecip_mm\": 0.0,\n" +
                "          \"daily_chance_of_rain\": 15,\n" +
                "          \"condition\": {\"text\": \"Partly cloudy\", \"code\": 1003}\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
    }

    @Test
    @DisplayName("Successful WeatherAPI response correctly maps all telemetry, conditions, and 5-day forecast")
    void testSuccessfulWeatherApiCityLookupAndMapping() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9124, 75.7873,
                        31.5, 32.7, 48, 0.0, 14.5, 210.0, 25, "Partly cloudy", 1003, true), MediaType.APPLICATION_JSON));

        WeatherResponse resp1 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(resp1);
        assertEquals("Jaipur, Rajasthan", resp1.getLocation());
        assertEquals("India", resp1.getCountry());
        assertEquals(26.9124, resp1.getLatitude(), 0.001);
        assertEquals(75.7873, resp1.getLongitude(), 0.001);
        assertEquals(31.5, resp1.getTemperature());
        assertEquals(32.7, resp1.getFeelsLike());
        assertEquals(32.7, resp1.getApparentTemperature());
        assertEquals(48, resp1.getHumidity());
        assertEquals(0.0, resp1.getPrecipitation());
        assertEquals(0.0, resp1.getRainfall());
        assertEquals(14.5, resp1.getWindSpeed());
        assertEquals(210.0, resp1.getWindDirection());
        assertEquals(25, resp1.getCloudCover());
        assertEquals("Partly cloudy", resp1.getCondition());
        assertEquals("bi-cloud-sun-fill", resp1.getConditionIcon());
        assertEquals("LIVE", resp1.getDataSource());
        assertFalse(resp1.isCached());
        assertFalse(resp1.isStale());
        assertNotNull(resp1.getLastUpdated());
        assertNotNull(resp1.getIrrigationAdvice());
        assertNotNull(resp1.getForecast());
        assertEquals(5, resp1.getForecast().size());

        // Subsequent call within 10 minutes hits fresh cache without external HTTP call
        WeatherResponse resp2 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(resp2);
        assertEquals("Jaipur, Rajasthan", resp2.getLocation());
        assertEquals(31.5, resp2.getTemperature());
        assertTrue(resp2.isCached());
        assertEquals("CACHED", resp2.getDataSource());
        assertFalse(resp2.isStale());

        mockServer.verify();
    }

    @Test
    @DisplayName("Coordinate-based lookup queries WeatherAPI directly with latitude,longitude")
    void testCoordinateBasedLookup() {
        double lat = 26.9124;
        double lon = 75.7873;

        mockServer.expect(requestTo(containsString("q=26.9124%2C75.7873")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", lat, lon,
                        32.0, 33.0, 42, 0.0, 15.0, 180.0, 10, "Sunny", 1000, true), MediaType.APPLICATION_JSON));

        WeatherResponse gpsResp = weatherService.getWeatherByCoordinates(lat, lon, "Jaipur Farm");
        assertNotNull(gpsResp);
        assertEquals("Jaipur Farm", gpsResp.getLocation());
        assertEquals(32.0, gpsResp.getTemperature());
        assertEquals("LIVE", gpsResp.getDataSource());
        assertEquals("bi-sun-fill", gpsResp.getConditionIcon());
        mockServer.verify();

        // Fresh cache hit
        WeatherResponse cachedGps = weatherService.getWeatherByCoordinates(lat, lon, "Jaipur Farm");
        assertNotNull(cachedGps);
        assertTrue(cachedGps.isCached());
    }

    @Test
    @DisplayName("Invalid city name (error code 1006) throws ResourceNotFoundException without returning fake weather")
    void testInvalidCityThrowsResourceNotFound() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withBadRequest().body("{\"error\": {\"code\": 1006, \"message\": \"No matching location found.\"}}"));

        assertThrows(ResourceNotFoundException.class, () -> {
            weatherService.getWeatherByCity("NonexistentCity9999");
        });
        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 400 with unrelated error code (e.g. 1003) throws BadRequestException and is NOT classified as ResourceNotFound")
    void testHttp400WithUnrelatedErrorCodeThrowsBadRequest() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withBadRequest().body("{\"error\": {\"code\": 1003, \"message\": \"Parameter 'q' not provided.\"}}"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            weatherService.getWeatherByCity("QueryWithMissingParam");
        });

        assertInstanceOf(BadRequestException.class, ex, "Unrelated 400 error should throw BadRequestException");
        assertFalse(ex instanceof ResourceNotFoundException, "Unrelated 400 error should NOT be classified as ResourceNotFoundException");
        assertTrue(ex.getMessage().contains("Parameter 'q' not provided"), "Error message should contain provider detail");
        mockServer.verify();
    }

    @Test
    @DisplayName("Configured base URL is used with trailing slash normalization")
    void testConfiguredBaseUrlIsUsed() {
        // Configure custom base URL with trailing slash
        weatherService.setWeatherApiBaseUrl("https://custom-weather.smartkrishi.com/v1/");

        mockServer.expect(requestTo(containsString("https://custom-weather.smartkrishi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9124, 75.7873,
                        30.0, 31.0, 50, 0.0, 10.0, 180.0, 15, "Sunny", 1000, true), MediaType.APPLICATION_JSON));

        WeatherResponse resp = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(resp);
        assertEquals("Jaipur, Rajasthan", resp.getLocation());
        mockServer.verify();

        // Also verify the endpoint builder normalizes multiple trailing slashes
        weatherService.setWeatherApiBaseUrl("https://custom-weather.smartkrishi.com/v1///");
        assertEquals("https://custom-weather.smartkrishi.com/v1/forecast.json", weatherService.getForecastEndpointUrl());
    }

    @Test
    @DisplayName("WeatherAPI quota error (code 2007) returns 6-hour stale fallback when available")
    void testQuotaExceededWithStaleFallback() {
        // Step 1: Successful initial call to populate stale cache
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9124, 75.7873,
                        30.0, 31.0, 50, 0.0, 10.0, 120.0, 20, "Clear", 1000, true), MediaType.APPLICATION_JSON));

        WeatherResponse initial = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(initial);
        mockServer.verify();

        // Step 2: Clear fresh cache
        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Step 3: Simulate WeatherAPI HTTP 403 quota exceeded error
        mockServer.reset();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "60");
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withRawStatus(403).headers(headers).body("{\"error\": {\"code\": 2007, \"message\": \"API key has exceeded quota.\"}}"));

        // Step 4: Should return stale fallback gracefully
        WeatherResponse fallback = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fallback);
        assertEquals("Jaipur, Rajasthan", fallback.getLocation());
        assertEquals(30.0, fallback.getTemperature());
        assertTrue(fallback.isStale());
        assertTrue(fallback.isCached());
        assertEquals("STALE_FALLBACK", fallback.getDataSource());
        assertNotNull(fallback.getNotice());
        assertTrue(fallback.getNotice().contains("quota") || fallback.getNotice().contains("rate limit"));
        mockServer.verify();
    }

    @Test
    @DisplayName("WeatherAPI quota error without cached data throws user-friendly BadRequestException")
    void testQuotaExceededWithoutStaleFallbackThrowsBadRequest() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withRawStatus(403).body("{\"error\": {\"code\": 2007, \"message\": \"API key has exceeded quota.\"}}"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("NewCityWithoutCache");
        });

        assertTrue(ex.getMessage().contains("rate-limited") || ex.getMessage().contains("quota"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Authentication error (code 2006) returns stale fallback if cached, else throws BadRequestException")
    void testAuthenticationErrorHandling() {
        // Step 1: Seed Pune in cache
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Pune", "Maharashtra", 18.5204, 73.8567,
                        28.0, 29.0, 60, 0.0, 11.0, 150.0, 15, "Sunny", 1000, true), MediaType.APPLICATION_JSON));
        weatherService.getWeatherByCity("Pune");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Step 2: Simulate invalid key HTTP 401
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withRawStatus(401).body("{\"error\": {\"code\": 2006, \"message\": \"API key provided is invalid\"}}"));

        // For cached city Pune: should return stale fallback
        WeatherResponse fallback = weatherService.getWeatherByCity("Pune");
        assertNotNull(fallback);
        assertTrue(fallback.isStale());
        assertNotNull(fallback.getNotice());
        assertTrue(fallback.getNotice().contains("authentication failed"));
        mockServer.verify();

        // For an uncached city with auth error: should throw BadRequestException
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withRawStatus(401).body("{\"error\": {\"code\": 2006, \"message\": \"API key provided is invalid\"}}"));

        assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedCityWithAuthError");
        });
        mockServer.verify();
    }

    @Test
    @DisplayName("Missing WEATHERAPI_KEY returns stale fallback if available, else throws BadRequestException")
    void testMissingApiKeyHandling() {
        // Seed Agra in cache
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Agra", "Uttar Pradesh", 27.1767, 78.0081,
                        33.0, 34.0, 40, 0.0, 12.0, 190.0, 10, "Sunny", 1000, true), MediaType.APPLICATION_JSON));
        weatherService.getWeatherByCity("Agra");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Clear API key
        weatherService.setWeatherApiKey("");

        // Cached Agra should return stale fallback with missing key notice
        WeatherResponse stale = weatherService.getWeatherByCity("Agra");
        assertNotNull(stale);
        assertTrue(stale.isStale());
        assertTrue(stale.getNotice().contains("WEATHERAPI_KEY"));

        // Uncached city without key throws BadRequestException
        assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedWithoutKey");
        });
    }

    @Test
    @DisplayName("Network timeout returns stale fallback if cached, else throws BadRequestException")
    void testTimeoutWithStaleFallback() {
        // Seed Kota in cache
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Kota", "Rajasthan", 25.18, 75.83,
                        35.0, 36.0, 35, 0.0, 16.0, 200.0, 5, "Sunny", 1000, true), MediaType.APPLICATION_JSON));
        weatherService.getWeatherByCity("Kota");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Simulate network timeout
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out"); });

        WeatherResponse fallback = weatherService.getWeatherByCity("Kota");
        assertNotNull(fallback);
        assertTrue(fallback.isStale());
        assertEquals(35.0, fallback.getTemperature());
        mockServer.verify();

        // Uncached city with timeout throws BadRequestException
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Connection timed out"); });

        assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedCityWithTimeout");
        });
        mockServer.verify();
    }

    @Test
    @DisplayName("Repeated timeouts trigger short bounded backoff without external calls and without reporting as HTTP 429")
    void testRepeatedTimeoutsTriggerShortBoundedBackoff() {
        // Step 1: Seed Jaipur in cache
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9124, 75.7873,
                        32.0, 33.0, 40, 0.0, 10.0, 180.0, 10, "Sunny", 1000, true), MediaType.APPLICATION_JSON));
        weatherService.getWeatherByCity("Jaipur");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Timeout 1
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Timeout 1"); });
        WeatherResponse fb1 = weatherService.getWeatherByCity("Jaipur");
        assertTrue(fb1.isStale());
        assertEquals(1, weatherService.getConsecutiveTimeouts());
        assertFalse(weatherService.isTimeoutBackoffActive());
        assertFalse(weatherService.isRateLimited());
        mockServer.verify();

        // Timeout 2 (threshold reached)
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Timeout 2"); });
        WeatherResponse fb2 = weatherService.getWeatherByCity("Jaipur");
        assertTrue(fb2.isStale());
        assertEquals(2, weatherService.getConsecutiveTimeouts());
        assertTrue(weatherService.isTimeoutBackoffActive());
        assertFalse(weatherService.isRateLimited(), "Timeouts must never be misreported as HTTP 429");
        mockServer.verify();

        // During backoff window: cached location returns stale immediately with zero external calls
        mockServer.reset();
        WeatherResponse fb3 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fb3);
        assertTrue(fb3.isStale());
        assertTrue(fb3.getNotice().contains("network timeouts") || fb3.getNotice().contains("timed out"));
        assertFalse(fb3.getNotice().contains("429"));
        mockServer.verify();

        // During backoff window: uncached location fails fast with zero external calls
        mockServer.reset();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedCityDuringBackoff");
        });
        assertTrue(ex.getMessage().contains("network timeouts"));
        assertFalse(ex.getMessage().contains("429"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Successful upstream call clears timeout backoff and resets consecutive timeout counter")
    void testTimeoutBackoffRecoveryOnSuccessfulUpstreamCall() {
        weatherService.recordTimeout();
        weatherService.recordTimeout();
        assertTrue(weatherService.isTimeoutBackoffActive());
        assertEquals(2, weatherService.getConsecutiveTimeouts());

        weatherService.clearTimeoutBackoff();
        assertFalse(weatherService.isTimeoutBackoffActive());
        assertEquals(0, weatherService.getConsecutiveTimeouts());

        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Surat", "Gujarat", 21.17, 72.83,
                        33.0, 34.0, 50, 0.0, 10.0, 150.0, 20, "Sunny", 1000, true), MediaType.APPLICATION_JSON));

        WeatherResponse resp = weatherService.getWeatherByCity("Surat");
        assertNotNull(resp);
        assertEquals("LIVE", resp.getDataSource());
        assertFalse(resp.isStale());
        assertFalse(weatherService.isTimeoutBackoffActive());
        assertEquals(0, weatherService.getConsecutiveTimeouts());
        mockServer.verify();
    }

    @Test
    @DisplayName("Respects Retry-After header and avoids external calls during 429 backoff")
    void testRespectsRetryAfterHeader() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Kota", "Rajasthan", 25.18, 75.83,
                        35.0, 36.0, 38, 0.0, 12.0, 190.0, 10, "Sunny", 1000, true), MediaType.APPLICATION_JSON));
        weatherService.getWeatherByCity("Kota");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // 429 with Retry-After 120s
        mockServer.reset();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "120");
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withRawStatus(429).headers(headers).body("{\"error\": {\"code\": 2007, \"message\": \"Rate limit\"}}"));

        WeatherResponse fb1 = weatherService.getWeatherByCity("Kota");
        assertTrue(fb1.isStale());
        assertTrue(weatherService.isRateLimited());
        mockServer.verify();

        // During backoff: zero external calls made
        mockServer.reset();
        WeatherResponse fb2 = weatherService.getWeatherByCity("Kota");
        assertNotNull(fb2);
        assertTrue(fb2.isStale());
        mockServer.verify();
    }

    @Test
    @DisplayName("Invalid coordinates reject with BadRequestException")
    void testInvalidCoordinatesValidation() {
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(95.0, 75.0, null));
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(-95.0, 75.0, null));
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(26.0, 190.0, null));
    }
}

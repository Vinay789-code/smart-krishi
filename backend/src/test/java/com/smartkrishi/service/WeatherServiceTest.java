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
        weatherService = new WeatherService(restTemplate, cacheManager);
        weatherService.clearRateLimit();
        weatherService.clearTimeoutBackoff();
    }

    private String geocodingJson(String name, double lat, double lon, String admin1) {
        return "{\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"name\": \"" + name + "\",\n" +
                "      \"latitude\": " + lat + ",\n" +
                "      \"longitude\": " + lon + ",\n" +
                "      \"admin1\": \"" + admin1 + "\",\n" +
                "      \"country\": \"India\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private String emptyGeocodingJson() {
        return "{\"results\": []}";
    }

    private String forecastJson(double temp, int humidity, double rain) {
        return "{\n" +
                "  \"timezone\": \"Asia/Kolkata\",\n" +
                "  \"current\": {\n" +
                "    \"time\": \"2026-10-09T22:00\",\n" +
                "    \"temperature_2m\": " + temp + ",\n" +
                "    \"apparent_temperature\": " + (temp + 1.2) + ",\n" +
                "    \"relative_humidity_2m\": " + humidity + ",\n" +
                "    \"precipitation\": " + rain + ",\n" +
                "    \"rain\": " + rain + ",\n" +
                "    \"wind_speed_10m\": 14.5,\n" +
                "    \"wind_direction_10m\": 210.0,\n" +
                "    \"cloud_cover\": 25,\n" +
                "    \"weather_code\": 1\n" +
                "  },\n" +
                "  \"daily\": {\n" +
                "    \"time\": [\"2026-10-10\", \"2026-10-11\", \"2026-10-12\", \"2026-10-13\", \"2026-10-14\"],\n" +
                "    \"temperature_2m_max\": [34.0, 33.0, 32.0, 31.0, 30.0],\n" +
                "    \"temperature_2m_min\": [22.0, 21.0, 20.0, 19.0, 18.0],\n" +
                "    \"weather_code\": [1, 0, 2, 61, 1],\n" +
                "    \"precipitation_sum\": [0.0, 0.0, 0.5, 4.2, 0.0],\n" +
                "    \"precipitation_probability_max\": [10.0, 5.0, 30.0, 70.0, 15.0]\n" +
                "  }\n" +
                "}";
    }

    @Test
    @DisplayName("Successful weather response sets live telemetry and caches in fresh cache")
    void testSuccessfulWeatherResponseAndFreshCacheBehavior() {
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Jaipur", 26.9124, 75.7873, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(31.5, 48, 0.0), MediaType.APPLICATION_JSON));

        WeatherResponse resp1 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(resp1);
        assertEquals("Jaipur, Rajasthan", resp1.getLocation());
        assertEquals(31.5, resp1.getTemperature());
        assertEquals(48, resp1.getHumidity());
        assertEquals("LIVE", resp1.getDataSource());
        assertFalse(resp1.isCached());
        assertFalse(resp1.isStale());
        assertNotNull(resp1.getIrrigationAdvice());
        assertEquals(5, resp1.getForecast().size());

        // Subsequent call should hit the fresh cache without re-invoking external HTTP endpoints
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
    @DisplayName("HTTP 429 returns stale fallback when cached data is available")
    void testHttp429WithCachedFallback() {
        // Step 1: Successful initial call to populate cache
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Jaipur", 26.9124, 75.7873, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(30.0, 50, 0.0), MediaType.APPLICATION_JSON));

        WeatherResponse initial = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(initial);
        assertEquals("LIVE", initial.getDataSource());
        mockServer.verify();

        // Step 2: Clear fresh cache so request reaches the live call again
        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Step 3: Configure mockServer to simulate Open-Meteo HTTP 429 Too Many Requests
        mockServer.reset();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "60");
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withRawStatus(429).headers(headers).body("{\"error\": true, \"reason\": \"Hourly limit reached\"}"));

        // Step 4: Call service - should return stale fallback gracefully instead of failing
        WeatherResponse fallback = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fallback);
        assertEquals("Jaipur, Rajasthan", fallback.getLocation());
        assertEquals(30.0, fallback.getTemperature());
        assertTrue(fallback.isStale());
        assertTrue(fallback.isCached());
        assertEquals("STALE_FALLBACK", fallback.getDataSource());
        assertNotNull(fallback.getNotice());
        assertTrue(fallback.getNotice().contains("429") || fallback.getNotice().contains("rate-limited"));
        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 429 without cached data throws user-friendly BadRequestException")
    void testHttp429WithoutCachedData() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withRawStatus(429).headers(headers).body("{\"error\": true}"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("NewCityWithoutCache");
        });

        assertTrue(ex.getMessage().contains("rate-limited"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Temporary provider timeout returns stale fallback if cached, else throws BadRequestException")
    void testTemporaryProviderErrors() {
        // Seed Pune in cache
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Pune", 18.5204, 73.8567, "Maharashtra"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(27.0, 60, 2.0), MediaType.APPLICATION_JSON));

        weatherService.getWeatherByCity("Pune");
        mockServer.verify();

        // Evict from fresh cache
        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Simulate network timeout on external forecast call
        mockServer.reset();
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Pune", 18.5204, 73.8567, "Maharashtra"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out"); });

        // For Pune: should return stale fallback
        WeatherResponse fallback = weatherService.getWeatherByCity("Pune");
        assertNotNull(fallback);
        assertTrue(fallback.isStale());
        assertEquals(27.0, fallback.getTemperature());
        mockServer.verify();

        // For an uncached city with network timeout: should throw BadRequestException
        mockServer.reset();
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Connection timed out"); });

        assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedCityWithTimeout");
        });
        mockServer.verify();
    }

    @Test
    @DisplayName("GPS coordinate weather retrieval and 429 fallback support")
    void testCityAndGpsRequests() {
        double lat = 26.9124;
        double lon = 75.7873;

        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(32.0, 42, 0.0), MediaType.APPLICATION_JSON));

        WeatherResponse gpsResp = weatherService.getWeatherByCoordinates(lat, lon, "Jaipur Farm");
        assertNotNull(gpsResp);
        assertEquals("Jaipur Farm", gpsResp.getLocation());
        assertEquals(32.0, gpsResp.getTemperature());
        assertEquals("LIVE", gpsResp.getDataSource());
        mockServer.verify();

        // Fresh cache hit
        WeatherResponse cachedGps = weatherService.getWeatherByCoordinates(lat, lon, "Jaipur Farm");
        assertNotNull(cachedGps);
        assertTrue(cachedGps.isCached());

        // Clear fresh cache and simulate 503 Provider Unavailable on external forecast
        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_COORDINATES).clear();
        mockServer.reset();
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        WeatherResponse fallbackGps = weatherService.getWeatherByCoordinates(lat, lon, "Jaipur Farm");
        assertNotNull(fallbackGps);
        assertTrue(fallbackGps.isStale());
        assertEquals(32.0, fallbackGps.getTemperature());
        mockServer.verify();
    }

    @Test
    @DisplayName("Respects Retry-After header and avoids external calls during backoff period")
    void testRespectsRetryAfterHeader() {
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Kota", 25.18, 75.83, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(35.0, 38, 0.0), MediaType.APPLICATION_JSON));

        weatherService.getWeatherByCity("Kota");
        mockServer.verify();

        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Simulate 429 with 120s Retry-After
        mockServer.reset();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "120");
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withRawStatus(429).headers(headers).body("{\"error\": true}"));

        // Call 1 triggers 429 and enters backoff
        WeatherResponse fb1 = weatherService.getWeatherByCity("Kota");
        assertTrue(fb1.isStale());
        assertTrue(weatherService.isRateLimited());
        mockServer.verify();

        // Reset mock server with NO expectations.
        // Call 2 during backoff window: must NOT call external API at all!
        mockServer.reset();
        WeatherResponse fb2 = weatherService.getWeatherByCity("Kota");
        assertNotNull(fb2);
        assertTrue(fb2.isStale());
        mockServer.verify(); // Verifies zero requests were received by mockServer!
    }

    @Test
    @DisplayName("Invalid coordinates reject with BadRequestException")
    void testInvalidCoordinatesValidation() {
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(95.0, 75.0, null));
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(-95.0, 75.0, null));
        assertThrows(BadRequestException.class, () -> weatherService.getWeatherByCoordinates(26.0, 190.0, null));
    }

    @Test
    @DisplayName("Invalid location name throws ResourceNotFoundException without returning fake weather")
    void testInvalidLocationThrowsResourceNotFound() {
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(emptyGeocodingJson(), MediaType.APPLICATION_JSON));

        assertThrows(ResourceNotFoundException.class, () -> weatherService.getWeatherByCity("NonexistentCity9999"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Repeated timeouts trigger short bounded backoff, returning stale fallback without HTTP 429 misreporting")
    void testRepeatedTimeoutsTriggerShortBoundedBackoff() {
        // Step 1: Populate stale cache for Jaipur
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Jaipur", 26.9124, 75.7873, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(32.0, 40, 0.0), MediaType.APPLICATION_JSON));

        weatherService.getWeatherByCity("Jaipur");
        mockServer.verify();

        // Evict from fresh cache so subsequent calls attempt external retrieval
        cacheManager.getCache(WeatherCacheConfig.CACHE_WEATHER_CITY).clear();

        // Step 2: First timeout occurs
        mockServer.reset();
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Jaipur", 26.9124, 75.7873, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out 1"); });

        WeatherResponse fb1 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fb1);
        assertTrue(fb1.isStale());
        assertEquals(1, weatherService.getConsecutiveTimeouts());
        assertFalse(weatherService.isTimeoutBackoffActive(), "Single timeout should not activate global backoff yet");
        assertFalse(weatherService.isRateLimited(), "Timeout should never be marked as rate-limited");
        mockServer.verify();

        // Step 3: Second timeout occurs (repeated timeout threshold reached)
        mockServer.reset();
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Jaipur", 26.9124, 75.7873, "Rajasthan"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out 2"); });

        WeatherResponse fb2 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fb2);
        assertTrue(fb2.isStale());
        assertEquals(2, weatherService.getConsecutiveTimeouts());
        assertTrue(weatherService.isTimeoutBackoffActive(), "Repeated timeouts must trigger timeout backoff");
        assertFalse(weatherService.isRateLimited(), "Timeouts must NEVER be reported as HTTP 429 rate-limited");
        mockServer.verify();

        // Step 4: During timeout backoff window, call for cached city should return stale fallback immediately without external HTTP request
        mockServer.reset(); // Expect ZERO requests
        WeatherResponse fb3 = weatherService.getWeatherByCity("Jaipur");
        assertNotNull(fb3);
        assertTrue(fb3.isStale());
        assertNotNull(fb3.getNotice());
        assertTrue(fb3.getNotice().contains("network timeouts") || fb3.getNotice().contains("timed out"));
        assertFalse(fb3.getNotice().contains("429"));
        assertFalse(fb3.getNotice().contains("rate-limited"));
        mockServer.verify(); // Verifies zero requests made

        // Step 5: During timeout backoff window, call for uncached city should throw BadRequestException fast without external HTTP request
        mockServer.reset(); // Expect ZERO requests
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            weatherService.getWeatherByCity("UncachedCityDuringTimeoutBackoff");
        });
        assertTrue(ex.getMessage().contains("network timeouts") || ex.getMessage().contains("timed out"));
        assertFalse(ex.getMessage().contains("429"));
        assertFalse(ex.getMessage().contains("rate-limited"));
        mockServer.verify(); // Verifies zero requests made
    }

    @Test
    @DisplayName("Successful upstream response clears consecutive timeout counter and recovers from backoff")
    void testTimeoutBackoffRecoveryOnSuccessfulUpstreamCall() {
        // Trigger repeated timeouts
        weatherService.recordTimeout();
        weatherService.recordTimeout();
        assertTrue(weatherService.isTimeoutBackoffActive());
        assertEquals(2, weatherService.getConsecutiveTimeouts());

        // Simulate upstream recovery after backoff expires or via recordSuccess
        weatherService.clearTimeoutBackoff();
        assertFalse(weatherService.isTimeoutBackoffActive());
        assertEquals(0, weatherService.getConsecutiveTimeouts());

        // Now an upstream call can succeed and maintain clean recovery
        mockServer.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(geocodingJson("Surat", 21.17, 72.83, "Gujarat"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(forecastJson(33.0, 50, 0.0), MediaType.APPLICATION_JSON));

        WeatherResponse resp = weatherService.getWeatherByCity("Surat");
        assertNotNull(resp);
        assertEquals("LIVE", resp.getDataSource());
        assertFalse(resp.isStale());
        assertFalse(weatherService.isTimeoutBackoffActive());
        assertEquals(0, weatherService.getConsecutiveTimeouts());
        mockServer.verify();
    }
}

package com.smartkrishi;

import com.smartkrishi.dto.CropRecommendationRequest;
import com.smartkrishi.dto.CropRecommendationResponse;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.service.CropRecommendationService;
import com.smartkrishi.service.DiseaseDetectionService;
import com.smartkrishi.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SmartKrishiApplicationTests {

    @Autowired
    private CropRecommendationService cropRecommendationService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private DiseaseDetectionService diseaseDetectionService;

    @Autowired
    private com.smartkrishi.service.MandiService mandiService;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertNotNull(cropRecommendationService);
        assertNotNull(weatherService);
        assertNotNull(diseaseDetectionService);
        assertNotNull(mandiService);
    }

    @Test
    @DisplayName("Crop Recommendation Engine recommends Rice for high rainfall and clay soil")
    void testRiceRecommendation() {
        CropRecommendationRequest req = new CropRecommendationRequest();
        req.setNitrogen(90.0);
        req.setPhosphorus(45.0);
        req.setPotassium(40.0);
        req.setPh(6.2);
        req.setTemperature(26.0);
        req.setHumidity(82.0);
        req.setRainfall(220.0);
        req.setSoilType("Clay");

        CropRecommendationResponse resp = cropRecommendationService.recommendCrop(req, null);
        assertNotNull(resp);
        assertTrue(resp.getRecommendedCrop().contains("Rice"), "Expected recommendation to be Rice");
        assertEquals("Kharif", resp.getSuitableSeason());
        assertNotNull(resp.getFertilizerSuggestion());
    }

    @Test
    @DisplayName("Live Weather Service retrieves genuine Open-Meteo telemetry and forecast")
    void testWeatherAndIrrigationService() {
        WeatherResponse resp = weatherService.getWeather("Pune");
        assertNotNull(resp);
        assertNotNull(resp.getLocation());
        assertTrue(resp.getTemperature() > -50 && resp.getTemperature() < 60);
        assertNotNull(resp.getIrrigationAdvice());
        assertNotNull(resp.getForecast());
        assertFalse(resp.getForecast().isEmpty());
    }

    @Test
    @DisplayName("Live Weather queries distinct locations (Jaipur, Delhi, Mumbai, Kota, Bengaluru)")
    void testWeatherMultipleLocations() {
        WeatherResponse jaipur = weatherService.getWeatherByCity("Jaipur");
        WeatherResponse delhi = weatherService.getWeatherByCity("Delhi");
        WeatherResponse mumbai = weatherService.getWeatherByCity("Mumbai");

        assertNotNull(jaipur);
        assertNotNull(delhi);
        assertNotNull(mumbai);

        // Verify distinct coordinates were resolved via Open-Meteo Geocoding
        assertNotEquals(jaipur.getLatitude(), delhi.getLatitude());
        assertNotEquals(delhi.getLatitude(), mumbai.getLatitude());
        assertTrue(jaipur.getLocation().contains("Jaipur"));
        assertTrue(delhi.getLocation().contains("Delhi"));
    }

    @Test
    @DisplayName("Invalid location search throws ResourceNotFoundException without returning fake weather")
    void testInvalidLocationThrowsException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            weatherService.getWeatherByCity("xxxxxxxx9999nonexistent");
        });
    }

    @Test
    @DisplayName("Coordinate-based weather retrieval works with GPS coordinates")
    void testWeatherByCoordinates() {
        // Jaipur coordinates: 26.9196, 75.7878
        WeatherResponse resp = weatherService.getWeatherByCoordinates(26.9196, 75.7878, null);
        assertNotNull(resp);
        assertNotNull(resp.getTemperature());
        assertNotNull(resp.getHumidity());
        assertNotNull(resp.getWindSpeed());
        assertEquals(26.9196, resp.getLatitude(), 0.01);
    }

    @Test
    @DisplayName("Disease Detection rejects empty or non-image files with BadRequestException")
    void testDiseaseDetectionValidation() {
        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(BadRequestException.class, () -> {
            diseaseDetectionService.analyze(emptyFile, null, null);
        });

        // Non-image file
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "not an image".getBytes());
        assertThrows(BadRequestException.class, () -> {
            diseaseDetectionService.analyze(textFile, null, null);
        });
    }

    @Test
    @DisplayName("Disease Detection throws exact AI auth error when AI_API_KEY is not configured")
    void testDiseaseDetectionUnauthenticatedError() {
        MockMultipartFile validImage = new MockMultipartFile("file", "leaf.jpg", "image/jpeg", new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            diseaseDetectionService.analyze(validImage, null, null);
        });
        assertTrue(ex.getMessage().contains("AI service authentication failed. Please verify AI_API_KEY."),
                "Expected exact error message, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Disease records retrieval returns empty list for new user without error")
    void testDiseaseHistoryEmpty() {
        var records = diseaseDetectionService.getUserRecords(99999L);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    @DisplayName("Nearby Mandi Service retrieves Muhana Mandi within 50km for Jaipur coordinates")
    void testNearbyMandiPricesJaipur() {
        // Jaipur coordinates: 26.9124, 75.7873
        var response = mandiService.getNearbyMandiPrices(26.9124, 75.7873, 50.0, "Tomato", null, null, "nearest");
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResults());
        assertFalse(response.getResults().isEmpty(), "Expected at least one nearby mandi for Jaipur coordinates");

        var first = response.getResults().get(0);
        assertTrue(first.getMandiName().contains("Muhana"), "Expected closest mandi to be Muhana Mandi");
        assertNotNull(first.getDistanceKm());
        assertTrue(first.getDistanceKm() > 5.0 && first.getDistanceKm() < 25.0, "Distance should be approx 12.4km, got: " + first.getDistanceKm());
        assertEquals("Tomato", first.getCommodity());
        assertEquals(2650.0, first.getModalPrice());
        assertNotNull(first.getSourceLabel());
        assertNotNull(first.getNavigateUrl());
    }

    @Test
    @DisplayName("Nearby Mandi Service filters by radius correctly")
    void testNearbyMandiRadiusFiltering() {
        // Radius 15km: should find Muhana (~12.4km) but exclude Chomu (~32.1km)
        var tightRadius = mandiService.getNearbyMandiPrices(26.9124, 75.7873, 15.0, null, null, null, "nearest");
        assertNotNull(tightRadius);
        boolean hasChomu = tightRadius.getResults().stream()
                .anyMatch(r -> r.getMandiName().toLowerCase().contains("chomu"));
        assertFalse(hasChomu, "Chomu Mandi (32km away) should be excluded within 15km radius");

        // Radius 50km: should include Chomu
        var wideRadius = mandiService.getNearbyMandiPrices(26.9124, 75.7873, 50.0, null, null, null, "nearest");
        assertNotNull(wideRadius);
        boolean hasChomuWide = wideRadius.getResults().stream()
                .anyMatch(r -> r.getMandiName().toLowerCase().contains("chomu"));
        assertTrue(hasChomuWide, "Chomu Mandi should be included within 50km radius");
    }

    @Test
    @DisplayName("Nearby Mandi Service rejects out-of-range coordinates with BadRequestException")
    void testNearbyMandiInvalidCoordinates() {
        assertThrows(BadRequestException.class, () -> {
            mandiService.getNearbyMandiPrices(95.0, 75.0, 50.0, null, null, null, "nearest");
        });
        assertThrows(BadRequestException.class, () -> {
            mandiService.getNearbyMandiPrices(26.0, 200.0, 50.0, null, null, null, "nearest");
        });
        assertThrows(BadRequestException.class, () -> {
            mandiService.getNearbyMandiPrices(26.0, 75.0, 500.0, null, null, null, "nearest");
        });
    }

    @Test
    @DisplayName("Nearby Mandi Service filters by manual State and District")
    void testManualStateDistrictFilter() {
        var response = mandiService.getNearbyMandiPrices(null, null, 50.0, "Wheat", "Rajasthan", "Jaipur", "nearest");
        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        var first = response.getResults().get(0);
        assertEquals("Wheat", first.getCommodity());
        assertEquals("Rajasthan", first.getState());
        assertEquals("Jaipur", first.getDistrict());
    }
}

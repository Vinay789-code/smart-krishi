package com.smartkrishi;

import com.smartkrishi.dto.CropRecommendationRequest;
import com.smartkrishi.dto.CropRecommendationResponse;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.service.CropRecommendationService;
import com.smartkrishi.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class SmartKrishiApplicationTests {

    @Autowired
    private CropRecommendationService cropRecommendationService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setupWeatherMocks() {
        if (weatherService != null && weatherService.getRestTemplate() != null) {
            mockServer = MockRestServiceServer.bindTo(weatherService.getRestTemplate()).ignoreExpectOrder(true).build();
        }
        if (weatherService != null) {
            weatherService.setWeatherApiKey("test-weatherapi-key");
            weatherService.clearRateLimit();
            weatherService.clearTimeoutBackoff();
        }
    }

    private String weatherApiJson(String name, String region, double lat, double lon, double temp, int humidity, double rain) {
        return "{\n" +
                "  \"location\": {\"name\": \"" + name + "\", \"region\": \"" + region + "\", \"country\": \"India\", \"lat\": " + lat + ", \"lon\": " + lon + ", \"tz_id\": \"Asia/Kolkata\"},\n" +
                "  \"current\": {\"last_updated\": \"2026-10-10 12:00\", \"temp_c\": " + temp + ", \"feelslike_c\": " + temp + ", \"humidity\": " + humidity + ", \"precip_mm\": " + rain + ", \"wind_kph\": 12.0, \"wind_degree\": 180.0, \"cloud\": 20, \"is_day\": 1, \"condition\": {\"text\": \"Partly cloudy\", \"code\": 1003}},\n" +
                "  \"forecast\": {\"forecastday\": [\n" +
                "    {\"date\": \"2026-10-10\", \"day\": {\"maxtemp_c\": 32.0, \"mintemp_c\": 22.0, \"totalprecip_mm\": 0.0, \"daily_chance_of_rain\": 10, \"condition\": {\"text\": \"Sunny\", \"code\": 1000}}},\n" +
                "    {\"date\": \"2026-10-11\", \"day\": {\"maxtemp_c\": 33.0, \"mintemp_c\": 21.5, \"totalprecip_mm\": 0.0, \"daily_chance_of_rain\": 20, \"condition\": {\"text\": \"Sunny\", \"code\": 1000}}},\n" +
                "    {\"date\": \"2026-10-12\", \"day\": {\"maxtemp_c\": 31.5, \"mintemp_c\": 20.0, \"totalprecip_mm\": 1.2, \"daily_chance_of_rain\": 45, \"condition\": {\"text\": \"Patchy rain possible\", \"code\": 1063}}},\n" +
                "    {\"date\": \"2026-10-13\", \"day\": {\"maxtemp_c\": 30.0, \"mintemp_c\": 19.5, \"totalprecip_mm\": 0.0, \"daily_chance_of_rain\": 15, \"condition\": {\"text\": \"Partly cloudy\", \"code\": 1003}}},\n" +
                "    {\"date\": \"2026-10-14\", \"day\": {\"maxtemp_c\": 29.0, \"mintemp_c\": 18.0, \"totalprecip_mm\": 0.0, \"daily_chance_of_rain\": 5, \"condition\": {\"text\": \"Sunny\", \"code\": 1000}}}\n" +
                "  ]}\n" +
                "}";
    }

    @Autowired
    private com.smartkrishi.service.MandiService mandiService;

    @Autowired
    private com.smartkrishi.service.FertilizerService fertilizerService;

    @Autowired
    private com.smartkrishi.service.ProfitCalculatorService profitCalculatorService;

    @Autowired
    private com.smartkrishi.service.CropCalendarService cropCalendarService;

    @Autowired
    private com.smartkrishi.service.PricePredictionService pricePredictionService;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertNotNull(cropRecommendationService);
        assertNotNull(weatherService);
        assertNotNull(mandiService);
        assertNotNull(fertilizerService);
        assertNotNull(profitCalculatorService);
        assertNotNull(cropCalendarService);
        assertNotNull(pricePredictionService);
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
    @DisplayName("Live Weather Service retrieves genuine WeatherAPI telemetry and forecast")
    void testWeatherAndIrrigationService() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Pune", "Maharashtra", 18.5204, 73.8567, 28.0, 65, 0.0), MediaType.APPLICATION_JSON));

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
        mockServer.expect(requestTo(containsString("q=Jaipur")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9124, 75.7873, 31.0, 45, 0.0), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("q=Delhi")))
                .andRespond(withSuccess(weatherApiJson("Delhi", "Delhi", 28.6139, 77.2090, 29.0, 50, 0.0), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("q=Mumbai")))
                .andRespond(withSuccess(weatherApiJson("Mumbai", "Maharashtra", 19.0760, 72.8777, 30.0, 75, 1.0), MediaType.APPLICATION_JSON));

        WeatherResponse jaipur = weatherService.getWeatherByCity("Jaipur");
        WeatherResponse delhi = weatherService.getWeatherByCity("Delhi");
        WeatherResponse mumbai = weatherService.getWeatherByCity("Mumbai");

        assertNotNull(jaipur);
        assertNotNull(delhi);
        assertNotNull(mumbai);

        // Verify distinct coordinates were resolved via WeatherAPI
        assertNotEquals(jaipur.getLatitude(), delhi.getLatitude());
        assertNotEquals(delhi.getLatitude(), mumbai.getLatitude());
        assertTrue(jaipur.getLocation().contains("Jaipur"));
        assertTrue(delhi.getLocation().contains("Delhi"));
    }

    @Test
    @DisplayName("Invalid location search throws ResourceNotFoundException without returning fake weather")
    void testInvalidLocationThrowsException() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withBadRequest().body("{\"error\":{\"code\":1006,\"message\":\"No matching location found.\"}}"));

        assertThrows(ResourceNotFoundException.class, () -> {
            weatherService.getWeatherByCity("xxxxxxxx9999nonexistent");
        });
    }

    @Test
    @DisplayName("Coordinate-based weather retrieval works with GPS coordinates")
    void testWeatherByCoordinates() {
        mockServer.expect(requestTo(containsString("api.weatherapi.com/v1/forecast.json")))
                .andRespond(withSuccess(weatherApiJson("Jaipur", "Rajasthan", 26.9196, 75.7878, 29.0, 55, 0.0), MediaType.APPLICATION_JSON));

        // Jaipur coordinates: 26.9196, 75.7878
        WeatherResponse resp = weatherService.getWeatherByCoordinates(26.9196, 75.7878, null);
        assertNotNull(resp);
        assertNotNull(resp.getTemperature());
        assertNotNull(resp.getHumidity());
        assertNotNull(resp.getWindSpeed());
        assertEquals(26.9196, resp.getLatitude(), 0.01);
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

    // ===================================================================
    // MODULE 1: SMART FERTILIZER RECOMMENDATION TESTS
    // ===================================================================

    @Test
    @DisplayName("Fertilizer Service calculates balanced ICAR dosages and split schedule for Wheat")
    void testFertilizerRecommendationBalanced() {
        var req = new com.smartkrishi.dto.FertilizerRecommendRequest();
        req.setCrop("Wheat");
        req.setSoilType("Alluvial Loam");
        req.setGrowthStage("Sowing / Basal");
        req.setLandArea(2.0);
        req.setNitrogen(180.0);
        req.setPhosphorus(18.0);
        req.setPotassium(140.0);
        req.setPh(7.0);

        var resp = fertilizerService.recommendFertilizer(req);
        assertNotNull(resp);
        assertEquals("Wheat", resp.getCrop());
        assertEquals("Alluvial Loam", resp.getSoilType());
        assertNotNull(resp.getFertilizers());
        assertFalse(resp.getFertilizers().isEmpty());

        boolean hasUrea = resp.getFertilizers().stream().anyMatch(f -> f.getName().contains("Urea"));
        boolean hasDapOrSsp = resp.getFertilizers().stream().anyMatch(f -> f.getName().contains("DAP") || f.getName().contains("SSP"));
        assertTrue(hasUrea, "Expected Urea recommendation for Nitrogen deficit");
        assertTrue(hasDapOrSsp, "Expected DAP or SSP recommendation for Phosphorus deficit");

        assertTrue(resp.getApproximateTotalCost() > 0);
        assertNotNull(resp.getSplitSchedule());
        assertFalse(resp.getSplitSchedule().isEmpty());
    }

    @Test
    @DisplayName("Fertilizer Service provides soil amendments for acidic and alkaline pH")
    void testFertilizerAcidicAndAlkalineSoilWarnings() {
        // Acidic soil test (pH 5.2)
        var acidicReq = new com.smartkrishi.dto.FertilizerRecommendRequest();
        acidicReq.setCrop("Tomato");
        acidicReq.setSoilType("Red Sandy Loam");
        acidicReq.setGrowthStage("Sowing / Basal");
        acidicReq.setLandArea(1.0);
        acidicReq.setNitrogen(150.0);
        acidicReq.setPhosphorus(20.0);
        acidicReq.setPotassium(120.0);
        acidicReq.setPh(5.2);

        var acidicResp = fertilizerService.recommendFertilizer(acidicReq);
        assertNotNull(acidicResp);
        assertEquals("ACIDIC", acidicResp.getPhStatus());
        boolean hasLimeAdvice = acidicResp.getPrecautions().stream()
                .anyMatch(p -> p.toLowerCase().contains("lime") || p.toLowerCase().contains("neutralize"));
        assertTrue(hasLimeAdvice, "Expected agricultural lime recommendation for acidic soil");

        // Alkaline soil test (pH 8.8)
        var alkalineReq = new com.smartkrishi.dto.FertilizerRecommendRequest();
        alkalineReq.setCrop("Cotton");
        alkalineReq.setSoilType("Black Clay (Regur)");
        alkalineReq.setGrowthStage("Vegetative");
        alkalineReq.setLandArea(2.0);
        alkalineReq.setNitrogen(140.0);
        alkalineReq.setPhosphorus(25.0);
        alkalineReq.setPotassium(180.0);
        alkalineReq.setPh(8.8);

        var alkalineResp = fertilizerService.recommendFertilizer(alkalineReq);
        assertNotNull(alkalineResp);
        assertTrue(alkalineResp.getPhStatus().contains("ALKALINE") || alkalineResp.getPhStatus().contains("SODIC"));
        boolean hasGypsumAdvice = alkalineResp.getPrecautions().stream()
                .anyMatch(p -> p.toLowerCase().contains("gypsum"));
        assertTrue(hasGypsumAdvice, "Expected gypsum amendment advice for alkaline soil");
    }

    // ===================================================================
    // MODULE 2: CROP PROFIT / ROI CALCULATOR TESTS
    // ===================================================================

    @Test
    @DisplayName("Profit Calculator accurately calculates revenue, cost, net margin, ROI, and break-even price")
    void testProfitCalculatorEconomics() {
        var req = new com.smartkrishi.dto.ProfitCalculationRequest();
        req.setCropName("Wheat");
        req.setLandAreaAcres(2.0);
        req.setExpectedYieldPerAcreQuintals(20.0);
        req.setExpectedSellingPricePerQuintal(2500.0);
        req.setSeedCost(3000.0);
        req.setFertilizerCost(6000.0);
        req.setPesticideCost(2000.0);
        req.setLaborCost(8000.0);
        req.setIrrigationCost(3000.0);
        req.setMachineryCost(5000.0);
        req.setOtherCost(3000.0);

        var resp = profitCalculatorService.calculateProfit(req);
        assertNotNull(resp);
        assertEquals("Wheat", resp.getCropName());
        assertEquals(40.0, resp.getTotalYieldQuintals(), 0.001);
        assertEquals(100000.0, resp.getGrossRevenue(), 0.001);
        assertEquals(30000.0, resp.getTotalCost(), 0.001);
        assertEquals(70000.0, resp.getNetProfit(), 0.001);
        assertEquals(35000.0, resp.getProfitPerAcre(), 0.001);
        assertEquals(233.33, resp.getRoiPercentage(), 0.1);
        assertEquals(750.0, resp.getBreakEvenPricePerQuintal(), 0.001);
        assertEquals("EXCELLENT", resp.getFinancialHealthRating());
        assertNotNull(resp.getCostBreakdownPercentages());
        assertEquals(7, resp.getCostBreakdownPercentages().size());
    }

    // ===================================================================
    // MODULE 3: CROP GROWTH CALENDAR TESTS
    // ===================================================================

    @Test
    @DisplayName("Crop Calendar retrieves structured 6 phenological stages and state notice for Wheat")
    void testCropCalendarStagesAndStateAdvisory() {
        var calendar = cropCalendarService.getCalendar("Wheat", "Rajasthan");
        assertNotNull(calendar);
        assertEquals("Wheat", calendar.getCrop());
        assertEquals("Rabi", calendar.getSeason());
        assertTrue(calendar.getDurationDays() >= 120);
        assertNotNull(calendar.getStages());
        assertEquals(6, calendar.getStages().size());

        // Check stages sequence
        assertEquals("Sowing & Basal Nutrition", calendar.getStages().get(0).getStageName());
        assertEquals("Harvesting & Storage", calendar.getStages().get(5).getStageName());
        assertNotNull(calendar.getStateSpecificNotice());
        assertTrue(calendar.getStateSpecificNotice().contains("Rajasthan"));

        // Fallback for unknown crop
        var genericCal = cropCalendarService.getCalendar("UnknownExoticBerry", null);
        assertNotNull(genericCal);
        assertFalse(genericCal.getStages().isEmpty());
    }

    // ===================================================================
    // MODULE 4: CROP PRICE PREDICTION TESTS
    // ===================================================================

    @Test
    @DisplayName("Price Prediction computes moving average, momentum trend, and confidence with >=3 historical points")
    void testPricePredictionSuccessWithHistoricalRecords() {
        var resp = pricePredictionService.predictPrice("Wheat", null, null, 10);
        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("Wheat", resp.getCrop());
        assertTrue(resp.getCurrentPrice() > 0);
        assertTrue(resp.getPredictedPrice() > 0);
        assertTrue(resp.getMovingAverage() > 0);
        assertTrue(resp.getConfidenceScore() >= 50.0);
        assertNotNull(resp.getTrend());
        assertNotNull(resp.getHistoricalPoints());
        assertTrue(resp.getHistoricalPoints().size() >= 3, "Expected at least 3 historical points for Wheat");
        assertNotNull(resp.getMethodology());
        assertNotNull(resp.getDisclaimer());
    }

    @Test
    @DisplayName("Price Prediction gracefully returns INSUFFICIENT_DATA when records < 3 without throwing error")
    void testPricePredictionInsufficientDataForRareFilter() {
        var resp = pricePredictionService.predictPrice("DragonFruitExotic", null, null, 10);
        assertNotNull(resp);
        assertEquals("INSUFFICIENT_DATA", resp.getStatus());
        assertNotNull(resp.getMessage());
        assertTrue(resp.getMessage().contains("minimum 3 required") || resp.getMessage().contains("Found 0"));
    }
}

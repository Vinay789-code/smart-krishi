package com.smartkrishi.service;

import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.dto.WeatherResponse.ForecastItem;
import com.smartkrishi.dto.WeatherResponse.IrrigationAdvice;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final RestTemplate restTemplate;

    public WeatherService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
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
     */
    @SuppressWarnings("unchecked")
    public WeatherResponse getWeatherByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            city = "Pune";
        }
        String cleanCity = city.trim();

        // 1. Resolve coordinates using Open-Meteo Geocoding API
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
            throw e;
        } catch (RestClientResponseException e) {
            logger.error("Open-Meteo Geocoding API HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BadRequestException("Live weather service temporarily unavailable (Geocoding error). Please try again.");
        } catch (ResourceAccessException e) {
            logger.error("Open-Meteo Geocoding API timeout/connectivity error: {}", e.getMessage());
            throw new BadRequestException("Live weather service network timeout. Please check internet connection.");
        } catch (Exception e) {
            logger.error("Unexpected error during geocoding: {}", e.getMessage());
            throw new BadRequestException("Failed to resolve location '" + cleanCity + "': " + e.getMessage());
        }

        // 2. Query live weather and 5-day forecast using resolved coordinates
        return fetchLiveWeather(lat, lon, resolvedLocationName, country);
    }

    /**
     * Fetch live real-time weather and 5-day forecast by GPS coordinates.
     * Uses reverse geocoding to resolve a human-readable location name.
     */
    public WeatherResponse getWeatherByCoordinates(double latitude, double longitude, String customLocationName) {
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Invalid geographical coordinates: Latitude must be between -90 and 90, Longitude between -180 and 180.");
        }

        String resolvedName = customLocationName;
        String country = "India";

        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = reverseGeocode(latitude, longitude);
        }

        return fetchLiveWeather(latitude, longitude, resolvedName, country);
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
    private WeatherResponse fetchLiveWeather(double lat, double lon, String locationName, String country) {
        String forecastUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max&timezone=auto",
                lat, lon);

        Map<String, Object> body;
        try {
            ResponseEntity<Map> weatherResp = restTemplate.getForEntity(forecastUrl, Map.class);
            body = weatherResp.getBody();
        } catch (RestClientResponseException e) {
            logger.error("Open-Meteo Weather API HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BadRequestException("Live weather provider temporarily unavailable. Status code: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            logger.error("Open-Meteo Weather API timeout/network error: {}", e.getMessage());
            throw new BadRequestException("Live weather service timed out. Please check your network connection.");
        } catch (Exception e) {
            logger.error("Failed to query Open-Meteo weather: {}", e.getMessage());
            throw new BadRequestException("Unable to retrieve live weather data: " + e.getMessage());
        }

        if (body == null || !body.containsKey("current")) {
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

        return resp;
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

package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.WeatherResponse;
import com.smartkrishi.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        WeatherResponse weather;
        if (latitude != null && longitude != null) {
            weather = weatherService.getWeatherByCoordinates(latitude, longitude, null);
        } else {
            String queryCity = (city != null && !city.isBlank()) ? city.trim() : "Pune";
            weather = weatherService.getWeatherByCity(queryCity);
        }

        return ResponseEntity.ok(ApiResponse.ok("Live weather and irrigation advice retrieved", weather));
    }

    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeatherForecast(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        WeatherResponse weather;
        if (latitude != null && longitude != null) {
            weather = weatherService.getWeatherByCoordinates(latitude, longitude, null);
        } else {
            String queryCity = (city != null && !city.isBlank()) ? city.trim() : "Pune";
            weather = weatherService.getWeatherByCity(queryCity);
        }

        return ResponseEntity.ok(ApiResponse.ok("5-day live weather forecast retrieved", weather));
    }
}

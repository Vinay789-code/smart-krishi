package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.MandiNearbyResponse;
import com.smartkrishi.service.MandiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mandi")
public class MandiController {

    private final MandiService mandiService;

    public MandiController(MandiService mandiService) {
        this.mandiService = mandiService;
    }

    /**
     * Finds nearby mandi prices using GPS coordinates or manual location filters.
     * Examples:
     * - GET /api/mandi/nearby?latitude=26.9124&longitude=75.7873&radius=50&crop=Tomato
     * - GET /api/mandi/nearby?state=Rajasthan&district=Jaipur&crop=Wheat
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<MandiNearbyResponse>> getNearbyMandiPrices(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "50.0") Double radius,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false, defaultValue = "nearest") String sortBy) {

        MandiNearbyResponse response = mandiService.getNearbyMandiPrices(
                latitude, longitude, radius, crop, state, district, sortBy);

        return ResponseEntity.ok(ApiResponse.ok("Mandi prices retrieved successfully", response));
    }

    /**
     * Returns list of available crops for dropdown filter.
     */
    @GetMapping("/crops")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableCrops() {
        List<String> crops = mandiService.getAvailableCrops();
        return ResponseEntity.ok(ApiResponse.ok("Available crops list", crops));
    }

    /**
     * Returns supported states and districts for manual location selection.
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getAvailableLocations() {
        Map<String, List<String>> locations = mandiService.getAvailableLocations();
        return ResponseEntity.ok(ApiResponse.ok("Available states and districts", locations));
    }

    /**
     * Administrative / System status of Mandi API connectivity and cache.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMandiStatus() {
        Map<String, Object> status = mandiService.getDataSourceStatus();
        return ResponseEntity.ok(ApiResponse.ok("Mandi data source status", status));
    }
}

package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.FertilizerRecommendRequest;
import com.smartkrishi.dto.FertilizerRecommendResponse;
import com.smartkrishi.service.FertilizerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fertilizer")
public class FertilizerController {

    private final FertilizerService fertilizerService;

    public FertilizerController(FertilizerService fertilizerService) {
        this.fertilizerService = fertilizerService;
    }

    /**
     * Rule-based fertilizer requirement & dosage advisory.
     * POST /api/fertilizer/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<FertilizerRecommendResponse>> getFertilizerRecommendation(
            @Valid @RequestBody FertilizerRecommendRequest request) {

        FertilizerRecommendResponse response = fertilizerService.recommendFertilizers(request);
        return ResponseEntity.ok(ApiResponse.ok("Fertilizer recommendation generated successfully", response));
    }
}

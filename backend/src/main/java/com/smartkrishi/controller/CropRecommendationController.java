package com.smartkrishi.controller;

import com.smartkrishi.config.JwtUtil;
import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.CropRecommendationRequest;
import com.smartkrishi.dto.CropRecommendationResponse;
import com.smartkrishi.service.CropRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
public class CropRecommendationController {

    private final CropRecommendationService cropRecommendationService;
    private final JwtUtil jwtUtil;

    public CropRecommendationController(CropRecommendationService cropRecommendationService, JwtUtil jwtUtil) {
        this.cropRecommendationService = cropRecommendationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<CropRecommendationResponse>> recommendCrop(
            @Valid @RequestBody CropRecommendationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                userId = jwtUtil.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {}
        }

        CropRecommendationResponse response = cropRecommendationService.recommendCrop(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Crop recommendation evaluated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropRecommendationResponse>>> getAllRecommendations() {
        List<CropRecommendationResponse> list = cropRecommendationService.getAllRecommendations();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CropRecommendationResponse>> getRecommendationById(@PathVariable Long id) {
        CropRecommendationResponse response = cropRecommendationService.getRecommendationById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

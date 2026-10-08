package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.PricePredictionResponse;
import com.smartkrishi.service.PricePredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/price-prediction")
public class PricePredictionController {

    private final PricePredictionService pricePredictionService;

    public PricePredictionController(PricePredictionService pricePredictionService) {
        this.pricePredictionService = pricePredictionService;
    }

    /**
     * Statistical baseline price prediction & market momentum analysis.
     * GET /api/price-prediction/{crop}?state={state}&district={district}&days={days}
     */
    @GetMapping("/{crop}")
    public ResponseEntity<ApiResponse<PricePredictionResponse>> getPricePrediction(
            @PathVariable String crop,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false, defaultValue = "10") Integer days) {

        PricePredictionResponse response = pricePredictionService.predictPrice(crop, state, district, days);
        return ResponseEntity.ok(ApiResponse.ok("Market price trend and prediction evaluated", response));
    }
}

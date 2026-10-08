package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.ProfitCalculationRequest;
import com.smartkrishi.dto.ProfitCalculationResponse;
import com.smartkrishi.service.ProfitCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profit")
public class ProfitCalculatorController {

    private final ProfitCalculatorService profitCalculatorService;

    public ProfitCalculatorController(ProfitCalculatorService profitCalculatorService) {
        this.profitCalculatorService = profitCalculatorService;
    }

    /**
     * Calculates farm production, operational expenditure, gross revenue, net profit, and ROI %.
     * POST /api/profit/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<ProfitCalculationResponse>> calculateProfit(
            @Valid @RequestBody ProfitCalculationRequest request) {

        ProfitCalculationResponse response = profitCalculatorService.calculateProfit(request);
        return ResponseEntity.ok(ApiResponse.ok("Profit and ROI calculated successfully", response));
    }
}

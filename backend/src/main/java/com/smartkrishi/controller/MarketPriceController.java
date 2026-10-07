package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.MarketPriceDto;
import com.smartkrishi.service.MarketPriceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    public MarketPriceController(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<List<MarketPriceDto>>> getMarketPrices(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district) {

        List<MarketPriceDto> prices;
        if (crop != null || state != null || district != null) {
            prices = marketPriceService.searchPrices(crop, state, district);
        } else {
            prices = marketPriceService.getAllPrices();
        }
        return ResponseEntity.ok(ApiResponse.ok("Market prices retrieved", prices));
    }

    @GetMapping("/prices/{crop}")
    public ResponseEntity<ApiResponse<List<MarketPriceDto>>> getPricesByCrop(@PathVariable String crop) {
        List<MarketPriceDto> prices = marketPriceService.getPricesByCrop(crop);
        return ResponseEntity.ok(ApiResponse.ok("Market prices for crop: " + crop, prices));
    }

    @GetMapping("/highlights")
    public ResponseEntity<ApiResponse<List<MarketPriceDto>>> getPriceHighlights() {
        List<MarketPriceDto> highlights = marketPriceService.getRecentHighlights();
        return ResponseEntity.ok(ApiResponse.ok("Recent market price highlights", highlights));
    }

    @PostMapping("/prices")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MarketPriceDto>> addMarketPrice(@Valid @RequestBody MarketPriceDto dto) {
        MarketPriceDto saved = marketPriceService.addPrice(dto);
        return new ResponseEntity<>(ApiResponse.ok("Market price added successfully", saved), HttpStatus.CREATED);
    }

    @DeleteMapping("/prices/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMarketPrice(@PathVariable Long id) {
        marketPriceService.deletePrice(id);
        return ResponseEntity.ok(ApiResponse.ok("Market price record deleted", null));
    }
}

package com.smartkrishi.controller;

import com.smartkrishi.dto.AdvisoryDto;
import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.service.AdvisoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advisory")
public class AdvisoryController {

    private final AdvisoryService advisoryService;

    public AdvisoryController(AdvisoryService advisoryService) {
        this.advisoryService = advisoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdvisoryDto>>> getAllAdvisories(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String season) {

        List<AdvisoryDto> advisories;
        if (category != null && !category.isBlank()) {
            advisories = advisoryService.getByCategory(category);
        } else if (season != null && !season.isBlank()) {
            advisories = advisoryService.getBySeason(season);
        } else {
            advisories = advisoryService.getAllAdvisories();
        }
        return ResponseEntity.ok(ApiResponse.ok("Advisories retrieved successfully", advisories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdvisoryDto>> getAdvisoryById(@PathVariable Long id) {
        AdvisoryDto advisory = advisoryService.getAdvisoryById(id);
        return ResponseEntity.ok(ApiResponse.ok("Advisory retrieved", advisory));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AdvisoryDto>>> getRecentAdvisories() {
        List<AdvisoryDto> list = advisoryService.getRecentAdvisories();
        return ResponseEntity.ok(ApiResponse.ok("Recent advisories", list));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AdvisoryDto>> addAdvisory(@Valid @RequestBody AdvisoryDto dto) {
        AdvisoryDto saved = advisoryService.addAdvisory(dto);
        return new ResponseEntity<>(ApiResponse.ok("Advisory created successfully", saved), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAdvisory(@PathVariable Long id) {
        advisoryService.deleteAdvisory(id);
        return ResponseEntity.ok(ApiResponse.ok("Advisory deleted successfully", null));
    }
}

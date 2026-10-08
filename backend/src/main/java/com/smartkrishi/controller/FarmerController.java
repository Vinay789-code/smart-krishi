package com.smartkrishi.controller;

import com.smartkrishi.dto.*;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.UnauthorizedException;
import com.smartkrishi.repository.UserRepository;
import com.smartkrishi.service.CropRecommendationService;
import com.smartkrishi.service.FarmerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {

    private final FarmerService farmerService;
    private final CropRecommendationService cropRecommendationService;
    private final UserRepository userRepository;

    public FarmerController(FarmerService farmerService,
                            CropRecommendationService cropRecommendationService,
                            UserRepository userRepository) {
        this.farmerService = farmerService;
        this.cropRecommendationService = cropRecommendationService;
        this.userRepository = userRepository;
    }

    private void verifyFarmerAccess(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) return;

            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user == null || !user.getId().equals(id)) {
                throw new UnauthorizedException("Access denied: You can only view and manage your own agricultural records.");
            }
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmerProfileDto>> getFarmerProfile(@PathVariable Long id) {
        verifyFarmerAccess(id);
        FarmerProfileDto profile = farmerService.getFarmerProfile(id);
        return ResponseEntity.ok(ApiResponse.ok("Farmer profile retrieved successfully", profile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmerProfileDto>> updateFarmerProfile(
            @PathVariable Long id,
            @RequestBody FarmerProfileDto dto) {
        verifyFarmerAccess(id);
        FarmerProfileDto updated = farmerService.updateFarmerProfile(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Farmer profile updated successfully", updated));
    }

    @PostMapping("/{id}/farms")
    public ResponseEntity<ApiResponse<FarmDto>> addFarm(
            @PathVariable Long id,
            @Valid @RequestBody FarmDto farmDto) {
        verifyFarmerAccess(id);
        FarmDto saved = farmerService.addFarm(id, farmDto);
        return new ResponseEntity<>(ApiResponse.ok("Farm added successfully", saved), HttpStatus.CREATED);
    }

    @DeleteMapping("/farms/{farmId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(@PathVariable Long farmId) {
        farmerService.deleteFarm(farmId);
        return ResponseEntity.ok(ApiResponse.ok("Farm removed successfully", null));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<CropRecommendationResponse>>> getFarmerRecommendations(@PathVariable Long id) {
        verifyFarmerAccess(id);
        List<CropRecommendationResponse> recs = cropRecommendationService.getUserRecommendations(id);
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }
}

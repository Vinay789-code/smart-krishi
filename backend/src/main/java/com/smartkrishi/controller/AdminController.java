package com.smartkrishi.controller;

import com.smartkrishi.dto.AdminStatsDto;
import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.FarmerProfileDto;
import com.smartkrishi.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getAdminStats() {
        AdminStatsDto stats = adminService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.ok("Admin statistics retrieved", stats));
    }

    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<FarmerProfileDto>>> getAllFarmers() {
        List<FarmerProfileDto> farmers = adminService.getAllFarmers();
        return ResponseEntity.ok(ApiResponse.ok("Registered farmers list", farmers));
    }

    @DeleteMapping("/farmers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarmer(@PathVariable Long id) {
        adminService.deleteFarmer(id);
        return ResponseEntity.ok(ApiResponse.ok("Farmer account deleted successfully", null));
    }
}

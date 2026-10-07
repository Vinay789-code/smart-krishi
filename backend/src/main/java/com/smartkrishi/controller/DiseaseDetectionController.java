package com.smartkrishi.controller;

import com.smartkrishi.config.JwtUtil;
import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.DiseaseAnalysisResponse;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.UnauthorizedException;
import com.smartkrishi.repository.UserRepository;
import com.smartkrishi.service.DiseaseDetectionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/disease")
public class DiseaseDetectionController {

    private final DiseaseDetectionService diseaseDetectionService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public DiseaseDetectionController(DiseaseDetectionService diseaseDetectionService,
                                      JwtUtil jwtUtil,
                                      UserRepository userRepository) {
        this.diseaseDetectionService = diseaseDetectionService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    private Long resolveUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }

    private String resolveUserRole(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractRole(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null && user.getRole() != null) {
                return user.getRole().name();
            }
        }
        return null;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DiseaseAnalysisResponse>> analyzeDisease(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cropHint", required = false) String cropHint,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = resolveUserId(authHeader);
        DiseaseAnalysisResponse response = diseaseDetectionService.analyze(file, cropHint, userId);
        return ResponseEntity.ok(ApiResponse.ok("Crop disease analysis completed successfully", response));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<DiseaseAnalysisResponse>>> getDiseaseRecords(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = resolveUserId(authHeader);
        String role = resolveUserRole(authHeader);

        if (userId == null) {
            throw new UnauthorizedException("Authentication token required to view disease scan records.");
        }

        List<DiseaseAnalysisResponse> list;
        if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            list = diseaseDetectionService.getAllRecords();
        } else {
            list = diseaseDetectionService.getUserRecords(userId);
        }
        return ResponseEntity.ok(ApiResponse.ok("Disease scan records retrieved successfully", list));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<DiseaseAnalysisResponse>>> getMyDiseaseHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = resolveUserId(authHeader);
        if (userId == null) {
            throw new UnauthorizedException("Authentication token required to view disease scan history.");
        }

        List<DiseaseAnalysisResponse> list = diseaseDetectionService.getUserRecords(userId);
        return ResponseEntity.ok(ApiResponse.ok("Personal disease scan history retrieved", list));
    }
}

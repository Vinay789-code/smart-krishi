package com.smartkrishi.service;

import com.smartkrishi.dto.AdminStatsDto;
import com.smartkrishi.dto.FarmerProfileDto;
import com.smartkrishi.entity.Role;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CropRecommendationRepository cropRecommendationRepository;
    private final DiseaseRecordRepository diseaseRecordRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final AdvisoryRepository advisoryRepository;
    private final FarmerService farmerService;
    private final CropRecommendationService cropRecommendationService;
    private final DiseaseDetectionService diseaseDetectionService;

    public AdminService(UserRepository userRepository,
                        CropRecommendationRepository cropRecommendationRepository,
                        DiseaseRecordRepository diseaseRecordRepository,
                        MarketPriceRepository marketPriceRepository,
                        AdvisoryRepository advisoryRepository,
                        FarmerService farmerService,
                        CropRecommendationService cropRecommendationService,
                        DiseaseDetectionService diseaseDetectionService) {
        this.userRepository = userRepository;
        this.cropRecommendationRepository = cropRecommendationRepository;
        this.diseaseRecordRepository = diseaseRecordRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.advisoryRepository = advisoryRepository;
        this.farmerService = farmerService;
        this.cropRecommendationService = cropRecommendationService;
        this.diseaseDetectionService = diseaseDetectionService;
    }

    public AdminStatsDto getAdminStats() {
        AdminStatsDto stats = new AdminStatsDto();
        stats.setTotalFarmers(userRepository.countByRole(Role.ROLE_FARMER));
        stats.setTotalRecommendations(cropRecommendationRepository.count());
        stats.setTotalDiseaseAnalyses(diseaseRecordRepository.count());
        stats.setTotalMarketRecords(marketPriceRepository.count());
        stats.setTotalAdvisories(advisoryRepository.count());
        stats.setRecentRecommendations(cropRecommendationService.getAllRecommendations().stream().limit(5).collect(Collectors.toList()));
        stats.setRecentDiseases(diseaseDetectionService.getAllRecords().stream().limit(5).collect(Collectors.toList()));
        return stats;
    }

    public List<FarmerProfileDto> getAllFarmers() {
        List<User> farmers = userRepository.findByRole(Role.ROLE_FARMER);
        return farmers.stream()
                .map(u -> farmerService.getFarmerProfile(u.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFarmer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with id: " + userId));
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot delete an administrator account");
        }
        userRepository.delete(user);
    }
}

package com.smartkrishi.service;

import com.smartkrishi.dto.*;
import com.smartkrishi.entity.Farm;
import com.smartkrishi.entity.FarmerProfile;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.FarmRepository;
import com.smartkrishi.repository.FarmerProfileRepository;
import com.smartkrishi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    private final UserRepository userRepository;
    private final FarmerProfileRepository profileRepository;
    private final FarmRepository farmRepository;

    public FarmerService(UserRepository userRepository,
                         FarmerProfileRepository profileRepository,
                         FarmRepository farmRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.farmRepository = farmRepository;
    }

    public FarmerProfileDto getFarmerProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer user not found with id: " + userId));

        FarmerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    FarmerProfile newProfile = new FarmerProfile();
                    newProfile.setUser(user);
                    return profileRepository.save(newProfile);
                });

        return mapToDto(user, profile);
    }

    @Transactional
    public FarmerProfileDto updateFarmerProfile(Long userId, FarmerProfileDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer user not found with id: " + userId));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.setPhone(dto.getPhone());
        }
        userRepository.save(user);

        FarmerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    FarmerProfile newProfile = new FarmerProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        if (dto.getVillage() != null) profile.setVillage(dto.getVillage());
        if (dto.getDistrict() != null) profile.setDistrict(dto.getDistrict());
        if (dto.getState() != null) profile.setState(dto.getState());
        if (dto.getLandArea() != null) profile.setLandArea(dto.getLandArea());
        if (dto.getSoilType() != null) profile.setSoilType(dto.getSoilType());
        if (dto.getIrrigationType() != null) profile.setIrrigationType(dto.getIrrigationType());
        if (dto.getPrimaryCrop() != null) profile.setPrimaryCrop(dto.getPrimaryCrop());

        FarmerProfile updatedProfile = profileRepository.save(profile);

        return mapToDto(user, updatedProfile);
    }

    @Transactional
    public FarmDto addFarm(Long userId, FarmDto dto) {
        FarmerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        Farm farm = new Farm(
                profile,
                dto.getFarmName(),
                dto.getPlotNumber(),
                dto.getAreaAcres() != null ? dto.getAreaAcres() : 0.0,
                dto.getSoilType() != null ? dto.getSoilType() : profile.getSoilType(),
                dto.getWaterSource() != null ? dto.getWaterSource() : profile.getIrrigationType()
        );

        Farm savedFarm = farmRepository.save(farm);
        return new FarmDto(
                savedFarm.getId(),
                savedFarm.getFarmName(),
                savedFarm.getPlotNumber(),
                savedFarm.getAreaAcres(),
                savedFarm.getSoilType(),
                savedFarm.getWaterSource()
        );
    }

    @Transactional
    public void deleteFarm(Long farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm not found with id: " + farmId);
        }
        farmRepository.deleteById(farmId);
    }

    private FarmerProfileDto mapToDto(User user, FarmerProfile profile) {
        FarmerProfileDto dto = new FarmerProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setVillage(profile.getVillage());
        dto.setDistrict(profile.getDistrict());
        dto.setState(profile.getState());
        dto.setLandArea(profile.getLandArea());
        dto.setSoilType(profile.getSoilType());
        dto.setIrrigationType(profile.getIrrigationType());
        dto.setPrimaryCrop(profile.getPrimaryCrop());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());

        List<Farm> farms = farmRepository.findByFarmerProfileId(profile.getId());
        dto.setFarms(farms.stream().map(f -> new FarmDto(
                f.getId(),
                f.getFarmName(),
                f.getPlotNumber(),
                f.getAreaAcres(),
                f.getSoilType(),
                f.getWaterSource()
        )).collect(Collectors.toList()));

        return dto;
    }
}

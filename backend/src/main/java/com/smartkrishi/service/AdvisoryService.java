package com.smartkrishi.service;

import com.smartkrishi.dto.AdvisoryDto;
import com.smartkrishi.entity.Advisory;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.AdvisoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdvisoryService {

    private final AdvisoryRepository advisoryRepository;

    public AdvisoryService(AdvisoryRepository advisoryRepository) {
        this.advisoryRepository = advisoryRepository;
    }

    public List<AdvisoryDto> getAllAdvisories() {
        return advisoryRepository.findAll()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<AdvisoryDto> getByCategory(String category) {
        return advisoryRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<AdvisoryDto> getBySeason(String season) {
        return advisoryRepository.findBySeasonIgnoreCase(season)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<AdvisoryDto> getRecentAdvisories() {
        return advisoryRepository.findTop4ByOrderByCreatedAtDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public AdvisoryDto getAdvisoryById(Long id) {
        Advisory advisory = advisoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisory not found with id: " + id));
        return mapToDto(advisory);
    }

    @Transactional
    public AdvisoryDto addAdvisory(AdvisoryDto dto) {
        Advisory entity = new Advisory();
        entity.setTitle(dto.getTitle());
        entity.setCategory(dto.getCategory().toUpperCase());
        entity.setSeason(dto.getSeason());
        entity.setTargetCrop(dto.getTargetCrop());
        entity.setSummary(dto.getSummary());
        entity.setDetails(dto.getDetails());
        entity.setApplicableState(dto.getApplicableState() != null ? dto.getApplicableState() : "All India");
        entity.setOfficialLink(dto.getOfficialLink());

        Advisory saved = advisoryRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteAdvisory(Long id) {
        if (!advisoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Advisory not found with id: " + id);
        }
        advisoryRepository.deleteById(id);
    }

    private AdvisoryDto mapToDto(Advisory entity) {
        AdvisoryDto dto = new AdvisoryDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCategory(entity.getCategory());
        dto.setSeason(entity.getSeason());
        dto.setTargetCrop(entity.getTargetCrop());
        dto.setSummary(entity.getSummary());
        dto.setDetails(entity.getDetails());
        dto.setApplicableState(entity.getApplicableState());
        dto.setOfficialLink(entity.getOfficialLink());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

package com.smartkrishi.service;

import com.smartkrishi.dto.MarketPriceDto;
import com.smartkrishi.entity.MarketPrice;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.MarketPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;

    public MarketPriceService(MarketPriceRepository marketPriceRepository) {
        this.marketPriceRepository = marketPriceRepository;
    }

    public List<MarketPriceDto> getAllPrices() {
        return marketPriceRepository.findAll()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<MarketPriceDto> searchPrices(String crop, String state, String district) {
        String cleanCrop = (crop != null && !crop.isBlank()) ? crop.trim() : null;
        String cleanState = (state != null && !state.isBlank()) ? state.trim() : null;
        String cleanDistrict = (district != null && !district.isBlank()) ? district.trim() : null;

        return marketPriceRepository.searchPrices(cleanCrop, cleanState, cleanDistrict)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<MarketPriceDto> getPricesByCrop(String crop) {
        return marketPriceRepository.findByCropNameIgnoreCase(crop)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<MarketPriceDto> getRecentHighlights() {
        return marketPriceRepository.findTop6ByOrderByPriceDateDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public MarketPriceDto addPrice(MarketPriceDto dto) {
        MarketPrice entity = new MarketPrice();
        entity.setCropName(dto.getCropName());
        entity.setVariety(dto.getVariety());
        entity.setMarketName(dto.getMarketName());
        entity.setDistrict(dto.getDistrict());
        entity.setState(dto.getState());
        entity.setMinPrice(dto.getMinPrice());
        entity.setMaxPrice(dto.getMaxPrice());
        entity.setModalPrice(dto.getModalPrice());
        entity.setUnit(dto.getUnit() != null ? dto.getUnit() : "₹/Quintal");
        entity.setPriceDate(dto.getPriceDate() != null ? dto.getPriceDate() : LocalDate.now());
        entity.setTrend(dto.getTrend() != null ? dto.getTrend() : "STABLE");

        MarketPrice saved = marketPriceRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deletePrice(Long id) {
        if (!marketPriceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Market price record not found with id: " + id);
        }
        marketPriceRepository.deleteById(id);
    }

    private MarketPriceDto mapToDto(MarketPrice entity) {
        MarketPriceDto dto = new MarketPriceDto();
        dto.setId(entity.getId());
        dto.setCropName(entity.getCropName());
        dto.setVariety(entity.getVariety());
        dto.setMarketName(entity.getMarketName());
        dto.setDistrict(entity.getDistrict());
        dto.setState(entity.getState());
        dto.setMinPrice(entity.getMinPrice());
        dto.setMaxPrice(entity.getMaxPrice());
        dto.setModalPrice(entity.getModalPrice());
        dto.setUnit(entity.getUnit());
        dto.setPriceDate(entity.getPriceDate());
        dto.setTrend(entity.getTrend());
        return dto;
    }
}

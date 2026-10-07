package com.smartkrishi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class MarketPriceDto {
    private Long id;

    @NotBlank(message = "Crop name is required")
    private String cropName;

    private String variety;

    @NotBlank(message = "Market/Mandi name is required")
    private String marketName;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "State is required")
    private String state;

    @NotNull(message = "Min price is required")
    @Positive(message = "Min price must be positive")
    private Double minPrice;

    @NotNull(message = "Max price is required")
    @Positive(message = "Max price must be positive")
    private Double maxPrice;

    @NotNull(message = "Modal price is required")
    @Positive(message = "Modal price must be positive")
    private Double modalPrice;

    private String unit = "₹/Quintal";
    private LocalDate priceDate;
    private String trend = "STABLE";

    public MarketPriceDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getMarketName() { return marketName; }
    public void setMarketName(String marketName) { this.marketName = marketName; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public Double getModalPrice() { return modalPrice; }
    public void setModalPrice(Double modalPrice) { this.modalPrice = modalPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }

    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
}

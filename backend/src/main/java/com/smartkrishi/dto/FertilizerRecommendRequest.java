package com.smartkrishi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FertilizerRecommendRequest {

    @NotBlank(message = "Crop name is required")
    private String crop;

    @NotBlank(message = "Soil type is required")
    private String soilType;

    @NotNull(message = "Nitrogen level is required")
    @DecimalMin(value = "0.0", message = "Nitrogen level cannot be negative")
    @DecimalMax(value = "500.0", message = "Nitrogen level exceeds valid range")
    private Double nitrogen;

    @NotNull(message = "Phosphorus level is required")
    @DecimalMin(value = "0.0", message = "Phosphorus level cannot be negative")
    @DecimalMax(value = "500.0", message = "Phosphorus level exceeds valid range")
    private Double phosphorus;

    @NotNull(message = "Potassium level is required")
    @DecimalMin(value = "0.0", message = "Potassium level cannot be negative")
    @DecimalMax(value = "500.0", message = "Potassium level exceeds valid range")
    private Double potassium;

    @NotNull(message = "pH value is required")
    @DecimalMin(value = "3.0", message = "pH cannot be below 3.0")
    @DecimalMax(value = "11.0", message = "pH cannot exceed 11.0")
    private Double ph;

    @NotNull(message = "Land area is required")
    @DecimalMin(value = "0.1", message = "Land area must be greater than 0.1 acres")
    @DecimalMax(value = "5000.0", message = "Land area exceeds supported range")
    private Double landArea;

    private String growthStage; // Basal, Vegetative, Flowering, Maturity

    public FertilizerRecommendRequest() {}

    public FertilizerRecommendRequest(String crop, String soilType, Double nitrogen, Double phosphorus,
                                      Double potassium, Double ph, Double landArea, String growthStage) {
        this.crop = crop;
        this.soilType = soilType;
        this.nitrogen = nitrogen;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
        this.ph = ph;
        this.landArea = landArea;
        this.growthStage = growthStage;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public Double getNitrogen() {
        return nitrogen;
    }

    public void setNitrogen(Double nitrogen) {
        this.nitrogen = nitrogen;
    }

    public Double getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(Double phosphorus) {
        this.phosphorus = phosphorus;
    }

    public Double getPotassium() {
        return potassium;
    }

    public void setPotassium(Double potassium) {
        this.potassium = potassium;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getLandArea() {
        return landArea;
    }

    public void setLandArea(Double landArea) {
        this.landArea = landArea;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }
}

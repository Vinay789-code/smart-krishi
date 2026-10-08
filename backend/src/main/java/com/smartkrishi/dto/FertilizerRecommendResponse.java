package com.smartkrishi.dto;

import java.util.ArrayList;
import java.util.List;

public class FertilizerRecommendResponse {

    private String crop;
    private String soilType;
    private String growthStage;
    private double landArea;

    private String nitrogenStatus;     // DEFICIENT, OPTIMAL, EXCESS
    private String phosphorusStatus;   // DEFICIENT, OPTIMAL, EXCESS
    private String potassiumStatus;    // DEFICIENT, OPTIMAL, EXCESS
    private String phStatus;           // ACIDIC, OPTIMAL_NEUTRAL, ALKALINE, SALINE_SODIC

    private List<FertilizerItemDto> recommendedFertilizers = new ArrayList<>();
    private String approximateQuantitySummary;
    private Double estimatedTotalCost;

    private List<String> applicationAdvice = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String methodology = "ICAR Agronomic Benchmark & Soil Nutrient Balance Rule Engine";
    private String disclaimer = "Advisory only. Exact nutrient needs vary by field history, crop cultivar, and seasonal rainfall. Follow local Krishi Vigyan Kendra (KVK) guidelines and official fertilizer label instructions.";

    public FertilizerRecommendResponse() {}

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

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public double getLandArea() {
        return landArea;
    }

    public void setLandArea(double landArea) {
        this.landArea = landArea;
    }

    public String getNitrogenStatus() {
        return nitrogenStatus;
    }

    public void setNitrogenStatus(String nitrogenStatus) {
        this.nitrogenStatus = nitrogenStatus;
    }

    public String getPhosphorusStatus() {
        return phosphorusStatus;
    }

    public void setPhosphorusStatus(String phosphorusStatus) {
        this.phosphorusStatus = phosphorusStatus;
    }

    public String getPotassiumStatus() {
        return potassiumStatus;
    }

    public void setPotassiumStatus(String potassiumStatus) {
        this.potassiumStatus = potassiumStatus;
    }

    public String getPhStatus() {
        return phStatus;
    }

    public void setPhStatus(String phStatus) {
        this.phStatus = phStatus;
    }

    public List<FertilizerItemDto> getRecommendedFertilizers() {
        return recommendedFertilizers;
    }

    public void setRecommendedFertilizers(List<FertilizerItemDto> recommendedFertilizers) {
        this.recommendedFertilizers = recommendedFertilizers;
    }

    public String getApproximateQuantitySummary() {
        return approximateQuantitySummary;
    }

    public void setApproximateQuantitySummary(String approximateQuantitySummary) {
        this.approximateQuantitySummary = approximateQuantitySummary;
    }

    public Double getEstimatedTotalCost() {
        return estimatedTotalCost;
    }

    public void setEstimatedTotalCost(Double estimatedTotalCost) {
        this.estimatedTotalCost = estimatedTotalCost;
    }

    public List<String> getApplicationAdvice() {
        return applicationAdvice;
    }

    public void setApplicationAdvice(List<String> applicationAdvice) {
        this.applicationAdvice = applicationAdvice;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getMethodology() {
        return methodology;
    }

    public void setMethodology(String methodology) {
        this.methodology = methodology;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public List<FertilizerItemDto> getFertilizers() {
        return recommendedFertilizers;
    }

    public Double getApproximateTotalCost() {
        return estimatedTotalCost;
    }

    public List<String> getSplitSchedule() {
        return applicationAdvice;
    }

    public List<String> getPrecautions() {
        return warnings;
    }
}

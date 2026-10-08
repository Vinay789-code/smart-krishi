package com.smartkrishi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProfitCalculationRequest {

    @NotBlank(message = "Crop name is required")
    private String crop;

    @NotNull(message = "Land area is required")
    @DecimalMin(value = "0.01", message = "Land area must be greater than 0")
    @DecimalMax(value = "10000.0", message = "Land area exceeds supported range")
    private Double landArea;

    @NotNull(message = "Expected yield per acre is required")
    @DecimalMin(value = "0.0", message = "Expected yield cannot be negative")
    private Double expectedYieldPerAcre;

    @NotNull(message = "Expected selling price is required")
    @DecimalMin(value = "0.0", message = "Expected selling price cannot be negative")
    private Double expectedSellingPrice;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double seedCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double fertilizerCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double pesticideCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double laborCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double irrigationCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double machineryCost = 0.0;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private Double otherCost = 0.0;

    public ProfitCalculationRequest() {}

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getCropName() {
        return crop;
    }

    public void setCropName(String cropName) {
        this.crop = cropName;
    }

    public Double getLandArea() {
        return landArea;
    }

    public void setLandArea(Double landArea) {
        this.landArea = landArea;
    }

    public Double getLandAreaAcres() {
        return landArea;
    }

    public void setLandAreaAcres(Double landAreaAcres) {
        this.landArea = landAreaAcres;
    }

    public Double getExpectedYieldPerAcre() {
        return expectedYieldPerAcre;
    }

    public void setExpectedYieldPerAcre(Double expectedYieldPerAcre) {
        this.expectedYieldPerAcre = expectedYieldPerAcre;
    }

    public Double getExpectedYieldPerAcreQuintals() {
        return expectedYieldPerAcre;
    }

    public void setExpectedYieldPerAcreQuintals(Double expectedYieldPerAcreQuintals) {
        this.expectedYieldPerAcre = expectedYieldPerAcreQuintals;
    }

    public Double getExpectedSellingPrice() {
        return expectedSellingPrice;
    }

    public void setExpectedSellingPrice(Double expectedSellingPrice) {
        this.expectedSellingPrice = expectedSellingPrice;
    }

    public Double getExpectedSellingPricePerQuintal() {
        return expectedSellingPrice;
    }

    public void setExpectedSellingPricePerQuintal(Double expectedSellingPricePerQuintal) {
        this.expectedSellingPrice = expectedSellingPricePerQuintal;
    }

    public Double getSeedCost() {
        return seedCost != null ? seedCost : 0.0;
    }

    public void setSeedCost(Double seedCost) {
        this.seedCost = seedCost != null ? seedCost : 0.0;
    }

    public Double getFertilizerCost() {
        return fertilizerCost != null ? fertilizerCost : 0.0;
    }

    public void setFertilizerCost(Double fertilizerCost) {
        this.fertilizerCost = fertilizerCost != null ? fertilizerCost : 0.0;
    }

    public Double getPesticideCost() {
        return pesticideCost != null ? pesticideCost : 0.0;
    }

    public void setPesticideCost(Double pesticideCost) {
        this.pesticideCost = pesticideCost != null ? pesticideCost : 0.0;
    }

    public Double getLaborCost() {
        return laborCost != null ? laborCost : 0.0;
    }

    public void setLaborCost(Double laborCost) {
        this.laborCost = laborCost != null ? laborCost : 0.0;
    }

    public Double getIrrigationCost() {
        return irrigationCost != null ? irrigationCost : 0.0;
    }

    public void setIrrigationCost(Double irrigationCost) {
        this.irrigationCost = irrigationCost != null ? irrigationCost : 0.0;
    }

    public Double getMachineryCost() {
        return machineryCost != null ? machineryCost : 0.0;
    }

    public void setMachineryCost(Double machineryCost) {
        this.machineryCost = machineryCost != null ? machineryCost : 0.0;
    }

    public Double getOtherCost() {
        return otherCost != null ? otherCost : 0.0;
    }

    public void setOtherCost(Double otherCost) {
        this.otherCost = otherCost != null ? otherCost : 0.0;
    }
}

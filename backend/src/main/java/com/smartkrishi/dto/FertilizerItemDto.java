package com.smartkrishi.dto;

public class FertilizerItemDto {

    private String name;
    private String nutrientProvided;
    private double dosePerAcreKg;
    private double totalQuantityKg;
    private int bagCount;
    private double bagSizeKg;
    private String applicationStage;
    private String applicationMethod;
    private double estimatedCost;

    public FertilizerItemDto() {}

    public FertilizerItemDto(String name, String nutrientProvided, double dosePerAcreKg,
                             double totalQuantityKg, int bagCount, double bagSizeKg,
                             String applicationStage, String applicationMethod, double estimatedCost) {
        this.name = name;
        this.nutrientProvided = nutrientProvided;
        this.dosePerAcreKg = dosePerAcreKg;
        this.totalQuantityKg = totalQuantityKg;
        this.bagCount = bagCount;
        this.bagSizeKg = bagSizeKg;
        this.applicationStage = applicationStage;
        this.applicationMethod = applicationMethod;
        this.estimatedCost = estimatedCost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNutrientProvided() {
        return nutrientProvided;
    }

    public void setNutrientProvided(String nutrientProvided) {
        this.nutrientProvided = nutrientProvided;
    }

    public double getDosePerAcreKg() {
        return dosePerAcreKg;
    }

    public void setDosePerAcreKg(double dosePerAcreKg) {
        this.dosePerAcreKg = dosePerAcreKg;
    }

    public double getTotalQuantityKg() {
        return totalQuantityKg;
    }

    public void setTotalQuantityKg(double totalQuantityKg) {
        this.totalQuantityKg = totalQuantityKg;
    }

    public int getBagCount() {
        return bagCount;
    }

    public void setBagCount(int bagCount) {
        this.bagCount = bagCount;
    }

    public double getBagSizeKg() {
        return bagSizeKg;
    }

    public void setBagSizeKg(double bagSizeKg) {
        this.bagSizeKg = bagSizeKg;
    }

    public String getApplicationStage() {
        return applicationStage;
    }

    public void setApplicationStage(String applicationStage) {
        this.applicationStage = applicationStage;
    }

    public String getApplicationMethod() {
        return applicationMethod;
    }

    public void setApplicationMethod(String applicationMethod) {
        this.applicationMethod = applicationMethod;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}

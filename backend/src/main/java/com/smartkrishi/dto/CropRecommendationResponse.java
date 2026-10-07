package com.smartkrishi.dto;

import java.time.LocalDateTime;

public class CropRecommendationResponse {
    private Long id;
    private String recommendedCrop;
    private String suitableSeason;
    private String cropDetails;
    private String fertilizerSuggestion;
    private String growingConditions;
    private Double nitrogen;
    private Double phosphorus;
    private Double potassium;
    private Double ph;
    private Double temperature;
    private Double humidity;
    private Double rainfall;
    private String soilType;
    private LocalDateTime createdAt;

    public CropRecommendationResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecommendedCrop() {
        return recommendedCrop;
    }

    public void setRecommendedCrop(String recommendedCrop) {
        this.recommendedCrop = recommendedCrop;
    }

    public String getSuitableSeason() {
        return suitableSeason;
    }

    public void setSuitableSeason(String suitableSeason) {
        this.suitableSeason = suitableSeason;
    }

    public String getCropDetails() {
        return cropDetails;
    }

    public void setCropDetails(String cropDetails) {
        this.cropDetails = cropDetails;
    }

    public String getFertilizerSuggestion() {
        return fertilizerSuggestion;
    }

    public void setFertilizerSuggestion(String fertilizerSuggestion) {
        this.fertilizerSuggestion = fertilizerSuggestion;
    }

    public String getGrowingConditions() {
        return growingConditions;
    }

    public void setGrowingConditions(String growingConditions) {
        this.growingConditions = growingConditions;
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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

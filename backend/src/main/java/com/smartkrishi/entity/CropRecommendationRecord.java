package com.smartkrishi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_recommendations")
public class CropRecommendationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private Double nitrogen;

    @Column(nullable = false)
    private Double phosphorus;

    @Column(nullable = false)
    private Double potassium;

    @Column(nullable = false)
    private Double ph;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false)
    private Double rainfall;

    @Column(name = "soil_type", nullable = false, length = 50)
    private String soilType;

    @Column(name = "recommended_crop", nullable = false, length = 100)
    private String recommendedCrop;

    @Column(name = "suitable_season", length = 50)
    private String suitableSeason;

    @Column(name = "fertilizer_suggestion", columnDefinition = "TEXT")
    private String fertilizerSuggestion;

    @Column(name = "growing_conditions", columnDefinition = "TEXT")
    private String growingConditions;

    @Column(name = "crop_details", columnDefinition = "TEXT")
    private String cropDetails;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public CropRecommendationRecord() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getCropDetails() {
        return cropDetails;
    }

    public void setCropDetails(String cropDetails) {
        this.cropDetails = cropDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

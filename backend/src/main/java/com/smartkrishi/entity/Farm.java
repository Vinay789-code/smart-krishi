package com.smartkrishi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "farms")
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_profile_id", nullable = false)
    @JsonIgnore
    private FarmerProfile farmerProfile;

    @Column(name = "farm_name", nullable = false, length = 100)
    private String farmName;

    @Column(name = "plot_number", length = 50)
    private String plotNumber;

    @Column(name = "area_acres", nullable = false)
    private Double areaAcres;

    @Column(name = "soil_type", length = 50)
    private String soilType;

    @Column(name = "water_source", length = 100)
    private String waterSource;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Farm() {}

    public Farm(FarmerProfile farmerProfile, String farmName, String plotNumber, Double areaAcres, String soilType, String waterSource) {
        this.farmerProfile = farmerProfile;
        this.farmName = farmName;
        this.plotNumber = plotNumber;
        this.areaAcres = areaAcres;
        this.soilType = soilType;
        this.waterSource = waterSource;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FarmerProfile getFarmerProfile() {
        return farmerProfile;
    }

    public void setFarmerProfile(FarmerProfile farmerProfile) {
        this.farmerProfile = farmerProfile;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getPlotNumber() {
        return plotNumber;
    }

    public void setPlotNumber(String plotNumber) {
        this.plotNumber = plotNumber;
    }

    public Double getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(Double areaAcres) {
        this.areaAcres = areaAcres;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public String getWaterSource() {
        return waterSource;
    }

    public void setWaterSource(String waterSource) {
        this.waterSource = waterSource;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

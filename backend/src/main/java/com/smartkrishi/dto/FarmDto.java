package com.smartkrishi.dto;

public class FarmDto {
    private Long id;
    private String farmName;
    private String plotNumber;
    private Double areaAcres;
    private String soilType;
    private String waterSource;

    public FarmDto() {}

    public FarmDto(Long id, String farmName, String plotNumber, Double areaAcres, String soilType, String waterSource) {
        this.id = id;
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
}

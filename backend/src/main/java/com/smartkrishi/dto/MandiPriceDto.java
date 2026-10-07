package com.smartkrishi.dto;

import java.io.Serializable;

public class MandiPriceDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String mandiName;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String commodity;
    private String variety;
    private String grade;
    private String arrivalDate;
    private Double minPrice;
    private Double maxPrice;
    private Double modalPrice;
    private String unit = "₹/quintal";
    private String source;
    private String sourceLabel;
    private String sourceType; // "GOVERNMENT_API", "CACHED_GOVERNMENT", "ADMIN_DATABASE"
    private String lastUpdated;
    private String navigateUrl;

    public MandiPriceDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getCommodity() { return commodity; }
    public void setCommodity(String commodity) { this.commodity = commodity; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(String arrivalDate) { this.arrivalDate = arrivalDate; }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public Double getModalPrice() { return modalPrice; }
    public void setModalPrice(Double modalPrice) { this.modalPrice = modalPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getNavigateUrl() { return navigateUrl; }
    public void setNavigateUrl(String navigateUrl) { this.navigateUrl = navigateUrl; }
}

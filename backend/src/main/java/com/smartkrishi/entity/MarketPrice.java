package com.smartkrishi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_prices")
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    @Column(length = 100)
    private String variety;

    @Column(name = "market_name", nullable = false, length = 150)
    private String marketName;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "min_price", nullable = false)
    private Double minPrice;

    @Column(name = "max_price", nullable = false)
    private Double maxPrice;

    @Column(name = "modal_price", nullable = false)
    private Double modalPrice;

    @Column(length = 30)
    private String unit = "₹/Quintal";

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(length = 20)
    private String trend = "STABLE"; // RISING, FALLING, STABLE

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (priceDate == null) {
            priceDate = LocalDate.now();
        }
    }

    public MarketPrice() {}

    public MarketPrice(String cropName, String variety, String marketName, String district, String state,
                       Double minPrice, Double maxPrice, Double modalPrice, String unit, LocalDate priceDate, String trend) {
        this.cropName = cropName;
        this.variety = variety;
        this.marketName = marketName;
        this.district = district;
        this.state = state;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
        this.unit = unit != null ? unit : "₹/Quintal";
        this.priceDate = priceDate != null ? priceDate : LocalDate.now();
        this.trend = trend != null ? trend : "STABLE";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getModalPrice() {
        return modalPrice;
    }

    public void setModalPrice(Double modalPrice) {
        this.modalPrice = modalPrice;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

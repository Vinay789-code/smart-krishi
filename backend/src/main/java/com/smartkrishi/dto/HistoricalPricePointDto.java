package com.smartkrishi.dto;

import java.time.LocalDate;

public class HistoricalPricePointDto {

    private LocalDate date;
    private double modalPrice;
    private double minPrice;
    private double maxPrice;
    private String marketName;

    public HistoricalPricePointDto() {}

    public HistoricalPricePointDto(LocalDate date, double modalPrice, double minPrice, double maxPrice, String marketName) {
        this.date = date;
        this.modalPrice = modalPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.marketName = marketName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getModalPrice() {
        return modalPrice;
    }

    public void setModalPrice(double modalPrice) {
        this.modalPrice = modalPrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }
}

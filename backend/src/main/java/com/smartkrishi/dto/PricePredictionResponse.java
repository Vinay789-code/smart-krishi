package com.smartkrishi.dto;

import java.util.ArrayList;
import java.util.List;

public class PricePredictionResponse {

    private String status; // SUCCESS or INSUFFICIENT_DATA
    private String message;

    private String crop;
    private String location;
    private Double currentPrice;
    private Double predictedPrice;
    private Double lowerRange;
    private Double upperRange;
    private String trend; // INCREASING, STABLE, DECREASING
    private Double percentageChange;
    private int historicalDataPoints;
    private String confidenceLevel; // High, Moderate, Low
    private Double confidenceScore;
    private Double movingAverage;
    private String horizon; // e.g. "Next 7-15 Days"

    private String methodology = "Simple Moving Average (SMA) & Historical Rate-of-Change Momentum";
    private String disclaimer = "Market Price Trend & Prediction is a statistical estimate based on recent APMC modal rates. Prices fluctuate based on seasonal harvest arrivals, transport logistics, weather events, and government export policies. Not a guaranteed future price.";

    private List<HistoricalPricePointDto> historicalPrices = new ArrayList<>();

    public PricePredictionResponse() {}

    public static PricePredictionResponse insufficientData(String crop, String location, String message) {
        PricePredictionResponse res = new PricePredictionResponse();
        res.setStatus("INSUFFICIENT_DATA");
        res.setCrop(crop);
        res.setLocation(location);
        res.setMessage(message != null ? message : "Not enough historical market data to produce a reliable prediction.");
        return res;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(Double predictedPrice) {
        this.predictedPrice = predictedPrice;
    }

    public Double getLowerRange() {
        return lowerRange;
    }

    public void setLowerRange(Double lowerRange) {
        this.lowerRange = lowerRange;
    }

    public Double getUpperRange() {
        return upperRange;
    }

    public void setUpperRange(Double upperRange) {
        this.upperRange = upperRange;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public int getHistoricalDataPoints() {
        return historicalDataPoints;
    }

    public void setHistoricalDataPoints(int historicalDataPoints) {
        this.historicalDataPoints = historicalDataPoints;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getHorizon() {
        return horizon;
    }

    public void setHorizon(String horizon) {
        this.horizon = horizon;
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

    public List<HistoricalPricePointDto> getHistoricalPrices() {
        return historicalPrices;
    }

    public void setHistoricalPrices(List<HistoricalPricePointDto> historicalPrices) {
        this.historicalPrices = historicalPrices;
    }

    public Double getMovingAverage() {
        return movingAverage;
    }

    public void setMovingAverage(Double movingAverage) {
        this.movingAverage = movingAverage;
    }

    public Double getConfidenceScore() {
        if (confidenceScore != null) return confidenceScore;
        if (confidenceLevel != null && confidenceLevel.contains("High")) return 82.0;
        if (confidenceLevel != null && confidenceLevel.contains("Moderate")) return 68.0;
        return 52.0;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Double getPredictedMinPrice() {
        return lowerRange;
    }

    public Double getPredictedMaxPrice() {
        return upperRange;
    }

    public Double getChangePercentage() {
        return percentageChange;
    }

    public List<HistoricalPricePointDto> getHistoricalPoints() {
        return historicalPrices;
    }
}

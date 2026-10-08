package com.smartkrishi.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProfitCalculationResponse {

    private String crop;
    private double landArea;
    private double expectedYieldPerAcre;
    private double expectedSellingPrice;

    private double totalProduction;
    private double totalCost;
    private double expectedRevenue;
    private double estimatedProfit;
    private double profitPerAcre;
    private double roiPercentage;
    private double breakEvenPrice;

    private double costPerAcre;
    private double revenuePerAcre;

    // Financial health indicator
    private String financialHealth; // HIGHLY_PROFITABLE, MODERATE_PROFIT, BREAK_EVEN, LOSS_MAKING
    private String summaryNotice;

    // Breakdown for visual charts
    private Map<String, Double> costBreakdown = new LinkedHashMap<>();
    private Map<String, Double> costPercentageBreakdown = new LinkedHashMap<>();

    public ProfitCalculationResponse() {}

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public double getLandArea() {
        return landArea;
    }

    public void setLandArea(double landArea) {
        this.landArea = landArea;
    }

    public double getExpectedYieldPerAcre() {
        return expectedYieldPerAcre;
    }

    public void setExpectedYieldPerAcre(double expectedYieldPerAcre) {
        this.expectedYieldPerAcre = expectedYieldPerAcre;
    }

    public double getExpectedSellingPrice() {
        return expectedSellingPrice;
    }

    public void setExpectedSellingPrice(double expectedSellingPrice) {
        this.expectedSellingPrice = expectedSellingPrice;
    }

    public double getTotalProduction() {
        return totalProduction;
    }

    public void setTotalProduction(double totalProduction) {
        this.totalProduction = totalProduction;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getExpectedRevenue() {
        return expectedRevenue;
    }

    public void setExpectedRevenue(double expectedRevenue) {
        this.expectedRevenue = expectedRevenue;
    }

    public double getEstimatedProfit() {
        return estimatedProfit;
    }

    public void setEstimatedProfit(double estimatedProfit) {
        this.estimatedProfit = estimatedProfit;
    }

    public double getProfitPerAcre() {
        return profitPerAcre;
    }

    public void setProfitPerAcre(double profitPerAcre) {
        this.profitPerAcre = profitPerAcre;
    }

    public double getRoiPercentage() {
        return roiPercentage;
    }

    public void setRoiPercentage(double roiPercentage) {
        this.roiPercentage = roiPercentage;
    }

    public double getBreakEvenPrice() {
        return breakEvenPrice;
    }

    public void setBreakEvenPrice(double breakEvenPrice) {
        this.breakEvenPrice = breakEvenPrice;
    }

    public double getCostPerAcre() {
        return costPerAcre;
    }

    public void setCostPerAcre(double costPerAcre) {
        this.costPerAcre = costPerAcre;
    }

    public double getRevenuePerAcre() {
        return revenuePerAcre;
    }

    public void setRevenuePerAcre(double revenuePerAcre) {
        this.revenuePerAcre = revenuePerAcre;
    }

    public String getFinancialHealth() {
        return financialHealth;
    }

    public void setFinancialHealth(String financialHealth) {
        this.financialHealth = financialHealth;
    }

    public String getSummaryNotice() {
        return summaryNotice;
    }

    public void setSummaryNotice(String summaryNotice) {
        this.summaryNotice = summaryNotice;
    }

    public Map<String, Double> getCostBreakdown() {
        return costBreakdown;
    }

    public void setCostBreakdown(Map<String, Double> costBreakdown) {
        this.costBreakdown = costBreakdown;
    }

    public Map<String, Double> getCostPercentageBreakdown() {
        return costPercentageBreakdown;
    }

    public void setCostPercentageBreakdown(Map<String, Double> costPercentageBreakdown) {
        this.costPercentageBreakdown = costPercentageBreakdown;
    }

    public String getCropName() {
        return crop;
    }

    public double getTotalYieldQuintals() {
        return totalProduction;
    }

    public double getGrossRevenue() {
        return expectedRevenue;
    }

    public double getNetProfit() {
        return estimatedProfit;
    }

    public double getBreakEvenPricePerQuintal() {
        return breakEvenPrice;
    }

    public String getFinancialHealthRating() {
        if ("HIGHLY_PROFITABLE".equals(financialHealth)) return "EXCELLENT";
        if ("MODERATE_PROFIT".equals(financialHealth)) return "GOOD";
        if ("BREAK_EVEN".equals(financialHealth)) return "MODERATE";
        if ("LOSS_MAKING".equals(financialHealth)) return "HIGH_RISK";
        return financialHealth != null ? financialHealth : "MODERATE";
    }

    public Map<String, Double> getCostBreakdownPercentages() {
        return costPercentageBreakdown;
    }

    public String getAdvisory() {
        return summaryNotice;
    }
}

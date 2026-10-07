package com.smartkrishi.dto;

import java.util.List;

public class AdminStatsDto {
    private long totalFarmers;
    private long totalRecommendations;
    private long totalDiseaseAnalyses;
    private long totalMarketRecords;
    private long totalAdvisories;
    private List<CropRecommendationResponse> recentRecommendations;
    private List<DiseaseAnalysisResponse> recentDiseases;

    public AdminStatsDto() {}

    public long getTotalFarmers() { return totalFarmers; }
    public void setTotalFarmers(long totalFarmers) { this.totalFarmers = totalFarmers; }

    public long getTotalRecommendations() { return totalRecommendations; }
    public void setTotalRecommendations(long totalRecommendations) { this.totalRecommendations = totalRecommendations; }

    public long getTotalDiseaseAnalyses() { return totalDiseaseAnalyses; }
    public void setTotalDiseaseAnalyses(long totalDiseaseAnalyses) { this.totalDiseaseAnalyses = totalDiseaseAnalyses; }

    public long getTotalMarketRecords() { return totalMarketRecords; }
    public void setTotalMarketRecords(long totalMarketRecords) { this.totalMarketRecords = totalMarketRecords; }

    public long getTotalAdvisories() { return totalAdvisories; }
    public void setTotalAdvisories(long totalAdvisories) { this.totalAdvisories = totalAdvisories; }

    public List<CropRecommendationResponse> getRecentRecommendations() { return recentRecommendations; }
    public void setRecentRecommendations(List<CropRecommendationResponse> recentRecommendations) { this.recentRecommendations = recentRecommendations; }

    public List<DiseaseAnalysisResponse> getRecentDiseases() { return recentDiseases; }
    public void setRecentDiseases(List<DiseaseAnalysisResponse> recentDiseases) { this.recentDiseases = recentDiseases; }
}

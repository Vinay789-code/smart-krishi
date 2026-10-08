package com.smartkrishi.dto;

import java.util.ArrayList;
import java.util.List;

public class CropStageDto {

    private int stageOrder;
    private String stageName;
    private String timeWindow;      // e.g. "Day 1 – 15" or "Mid October to November"
    private String irrigationGuidance;
    private String fertilizerGuidance;
    private List<String> farmingActivities = new ArrayList<>();
    private List<String> precautions = new ArrayList<>();

    public CropStageDto() {}

    public CropStageDto(int stageOrder, String stageName, String timeWindow,
                        String irrigationGuidance, String fertilizerGuidance,
                        List<String> farmingActivities, List<String> precautions) {
        this.stageOrder = stageOrder;
        this.stageName = stageName;
        this.timeWindow = timeWindow;
        this.irrigationGuidance = irrigationGuidance;
        this.fertilizerGuidance = fertilizerGuidance;
        this.farmingActivities = farmingActivities;
        this.precautions = precautions;
    }

    public int getStageOrder() {
        return stageOrder;
    }

    public void setStageOrder(int stageOrder) {
        this.stageOrder = stageOrder;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getIrrigationGuidance() {
        return irrigationGuidance;
    }

    public void setIrrigationGuidance(String irrigationGuidance) {
        this.irrigationGuidance = irrigationGuidance;
    }

    public String getFertilizerGuidance() {
        return fertilizerGuidance;
    }

    public void setFertilizerGuidance(String fertilizerGuidance) {
        this.fertilizerGuidance = fertilizerGuidance;
    }

    public List<String> getFarmingActivities() {
        return farmingActivities;
    }

    public void setFarmingActivities(List<String> farmingActivities) {
        this.farmingActivities = farmingActivities;
    }

    public List<String> getPrecautions() {
        return precautions;
    }

    public void setPrecautions(List<String> precautions) {
        this.precautions = precautions;
    }
}

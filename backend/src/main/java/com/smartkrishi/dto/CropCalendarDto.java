package com.smartkrishi.dto;

import java.util.ArrayList;
import java.util.List;

public class CropCalendarDto {

    private String crop;
    private String scientificName;
    private String season;               // Kharif, Rabi, Zaid
    private String totalDurationDays;     // e.g. "120 - 135 Days"
    private String idealTemperature;      // e.g. "15°C - 25°C"
    private String rainfallRequirement;  // e.g. "450 - 650 mm"
    private String soilSuitability;       // e.g. "Well-drained Loamy to Clay Loam"
    private String state;                 // State requested or "All India Baseline"
    private boolean isStateSpecific;

    private List<CropStageDto> stages = new ArrayList<>();
    private List<String> generalAdvisories = new ArrayList<>();
    private String notice;
    private String disclaimer = "Advisory only. Crop phenology, sowing windows, and pest pressures shift with local weather, seed cultivar, and altitude. Consult your district Agriculture Officer or local Krishi Vigyan Kendra (KVK).";

    public CropCalendarDto() {}

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getTotalDurationDays() {
        return totalDurationDays;
    }

    public void setTotalDurationDays(String totalDurationDays) {
        this.totalDurationDays = totalDurationDays;
    }

    public String getIdealTemperature() {
        return idealTemperature;
    }

    public void setIdealTemperature(String idealTemperature) {
        this.idealTemperature = idealTemperature;
    }

    public String getRainfallRequirement() {
        return rainfallRequirement;
    }

    public void setRainfallRequirement(String rainfallRequirement) {
        this.rainfallRequirement = rainfallRequirement;
    }

    public String getSoilSuitability() {
        return soilSuitability;
    }

    public void setSoilSuitability(String soilSuitability) {
        this.soilSuitability = soilSuitability;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isStateSpecific() {
        return isStateSpecific;
    }

    public void setStateSpecific(boolean stateSpecific) {
        isStateSpecific = stateSpecific;
    }

    public List<CropStageDto> getStages() {
        return stages;
    }

    public void setStages(List<CropStageDto> stages) {
        this.stages = stages;
    }

    public List<String> getGeneralAdvisories() {
        return generalAdvisories;
    }

    public void setGeneralAdvisories(List<String> generalAdvisories) {
        this.generalAdvisories = generalAdvisories;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getBotanicalName() {
        return scientificName;
    }

    public void setBotanicalName(String botanicalName) {
        this.scientificName = botanicalName;
    }

    public Integer getDurationDays() {
        if (totalDurationDays == null) return 120;
        try {
            String digits = totalDurationDays.replaceAll("[^0-9]", " ").trim();
            String[] parts = digits.split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception ignored) {}
        return 120;
    }

    public String getStateSpecificNotice() {
        return notice;
    }

    public void setStateSpecificNotice(String stateSpecificNotice) {
        this.notice = stateSpecificNotice;
    }
}

package com.smartkrishi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CropRecommendationRequest {

    @NotNull(message = "Nitrogen (N) value is required")
    @DecimalMin(value = "0.0", message = "Nitrogen must be positive")
    private Double nitrogen;

    @NotNull(message = "Phosphorus (P) value is required")
    @DecimalMin(value = "0.0", message = "Phosphorus must be positive")
    private Double phosphorus;

    @NotNull(message = "Potassium (K) value is required")
    @DecimalMin(value = "0.0", message = "Potassium must be positive")
    private Double potassium;

    @NotNull(message = "Soil pH value is required")
    @DecimalMin(value = "3.0", message = "pH must be at least 3.0")
    @DecimalMax(value = "10.0", message = "pH must be at most 10.0")
    private Double ph;

    @NotNull(message = "Temperature value is required")
    private Double temperature;

    @NotNull(message = "Humidity value is required")
    @DecimalMin(value = "0.0", message = "Humidity must be at least 0%")
    @DecimalMax(value = "100.0", message = "Humidity cannot exceed 100%")
    private Double humidity;

    @NotNull(message = "Rainfall value is required")
    @DecimalMin(value = "0.0", message = "Rainfall must be positive")
    private Double rainfall;

    @NotBlank(message = "Soil type is required")
    private String soilType;

    public CropRecommendationRequest() {}

    public Double getNitrogen() {
        return nitrogen;
    }

    public void setNitrogen(Double nitrogen) {
        this.nitrogen = nitrogen;
    }

    public Double getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(Double phosphorus) {
        this.phosphorus = phosphorus;
    }

    public Double getPotassium() {
        return potassium;
    }

    public void setPotassium(Double potassium) {
        this.potassium = potassium;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }
}

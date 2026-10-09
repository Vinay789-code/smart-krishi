package com.smartkrishi.dto;

import java.util.ArrayList;
import java.util.List;

public class WeatherResponse {
    private String location;
    private String country = "India";
    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double feelsLike;
    private Double apparentTemperature;
    private String condition;
    private String conditionIcon;
    private Integer humidity;
    private Double rainfall;
    private Double precipitation;
    private Double windSpeed;
    private Double windDirection;
    private Integer cloudCover;
    private String timezone;
    private String lastUpdated;
    private IrrigationAdvice irrigationAdvice;
    private List<ForecastItem> forecast = new ArrayList<>();

    // Reliability & caching metadata
    private String dataSource = "LIVE";
    private boolean cached = false;
    private boolean stale = false;
    private String notice;
    private Long cachedAt;

    public static class ForecastItem {
        private String date;
        private String dayOfWeek;
        private Double tempMin;
        private Double tempMax;
        private String condition;
        private String conditionIcon;
        private Double precipitationProbability;
        private Double rainAmountMm;

        public ForecastItem() {}

        public ForecastItem(String date, String dayOfWeek, Double tempMin, Double tempMax,
                            String condition, String conditionIcon, Double precipitationProbability, Double rainAmountMm) {
            this.date = date;
            this.dayOfWeek = dayOfWeek;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.condition = condition;
            this.conditionIcon = conditionIcon;
            this.precipitationProbability = precipitationProbability;
            this.rainAmountMm = rainAmountMm;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public Double getTempMin() { return tempMin; }
        public void setTempMin(Double tempMin) { this.tempMin = tempMin; }

        public Double getTempMax() { return tempMax; }
        public void setTempMax(Double tempMax) { this.tempMax = tempMax; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getConditionIcon() { return conditionIcon; }
        public void setConditionIcon(String conditionIcon) { this.conditionIcon = conditionIcon; }

        public Double getPrecipitationProbability() { return precipitationProbability; }
        public void setPrecipitationProbability(Double precipitationProbability) { this.precipitationProbability = precipitationProbability; }

        public Double getRainAmountMm() { return rainAmountMm; }
        public void setRainAmountMm(Double rainAmountMm) { this.rainAmountMm = rainAmountMm; }
    }

    public static class IrrigationAdvice {
        private String status; // "NO_IRRIGATION", "MODERATE_IRRIGATION", "HEAVY_IRRIGATION", "LIGHT_IRRIGATION"
        private String badgeColor; // success, warning, danger, info
        private String headline;
        private String detailedAdvice;
        private String bestTime;
        private String soilMoistureEstimation;

        public IrrigationAdvice() {}

        public IrrigationAdvice(String status, String badgeColor, String headline, String detailedAdvice, String bestTime, String soilMoistureEstimation) {
            this.status = status;
            this.badgeColor = badgeColor;
            this.headline = headline;
            this.detailedAdvice = detailedAdvice;
            this.bestTime = bestTime;
            this.soilMoistureEstimation = soilMoistureEstimation;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getBadgeColor() { return badgeColor; }
        public void setBadgeColor(String badgeColor) { this.badgeColor = badgeColor; }

        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }

        public String getDetailedAdvice() { return detailedAdvice; }
        public void setDetailedAdvice(String detailedAdvice) { this.detailedAdvice = detailedAdvice; }

        public String getBestTime() { return bestTime; }
        public void setBestTime(String bestTime) { this.bestTime = bestTime; }

        public String getSoilMoistureEstimation() { return soilMoistureEstimation; }
        public void setSoilMoistureEstimation(String soilMoistureEstimation) { this.soilMoistureEstimation = soilMoistureEstimation; }
    }

    public WeatherResponse() {}

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(Double feelsLike) { this.feelsLike = feelsLike; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getConditionIcon() { return conditionIcon; }
    public void setConditionIcon(String conditionIcon) { this.conditionIcon = conditionIcon; }

    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }

    public Double getRainfall() { return rainfall; }
    public void setRainfall(Double rainfall) { this.rainfall = rainfall; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public Integer getCloudCover() { return cloudCover; }
    public void setCloudCover(Integer cloudCover) { this.cloudCover = cloudCover; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public IrrigationAdvice getIrrigationAdvice() { return irrigationAdvice; }
    public void setIrrigationAdvice(IrrigationAdvice irrigationAdvice) { this.irrigationAdvice = irrigationAdvice; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getApparentTemperature() { return apparentTemperature != null ? apparentTemperature : feelsLike; }
    public void setApparentTemperature(Double apparentTemperature) {
        this.apparentTemperature = apparentTemperature;
        if (this.feelsLike == null) this.feelsLike = apparentTemperature;
    }

    public Double getPrecipitation() { return precipitation != null ? precipitation : rainfall; }
    public void setPrecipitation(Double precipitation) {
        this.precipitation = precipitation;
        if (this.rainfall == null) this.rainfall = precipitation;
    }

    public Double getWindDirection() { return windDirection; }
    public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public List<ForecastItem> getForecast() { return forecast; }
    public void setForecast(List<ForecastItem> forecast) { this.forecast = forecast; }

    public WeatherResponse(WeatherResponse other) {
        if (other == null) return;
        this.location = other.location;
        this.country = other.country;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.temperature = other.temperature;
        this.feelsLike = other.feelsLike;
        this.apparentTemperature = other.apparentTemperature;
        this.condition = other.condition;
        this.conditionIcon = other.conditionIcon;
        this.humidity = other.humidity;
        this.rainfall = other.rainfall;
        this.precipitation = other.precipitation;
        this.windSpeed = other.windSpeed;
        this.windDirection = other.windDirection;
        this.cloudCover = other.cloudCover;
        this.timezone = other.timezone;
        this.lastUpdated = other.lastUpdated;
        this.irrigationAdvice = other.irrigationAdvice;
        this.forecast = other.forecast != null ? new ArrayList<>(other.forecast) : new ArrayList<>();
        this.dataSource = other.dataSource;
        this.cached = other.cached;
        this.stale = other.stale;
        this.notice = other.notice;
        this.cachedAt = other.cachedAt;
    }

    public WeatherResponse copy() {
        return new WeatherResponse(this);
    }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }

    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }

    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }

    public Long getCachedAt() { return cachedAt; }
    public void setCachedAt(Long cachedAt) { this.cachedAt = cachedAt; }
}

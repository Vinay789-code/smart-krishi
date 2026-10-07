package com.smartkrishi.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MandiNearbyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success = true;
    private String source;
    private String sourceType; // GOVERNMENT_API, CACHED_GOVERNMENT, ADMIN_DATABASE
    private String sourceLabel;
    private String lastUpdated;
    private LocationInfo location;
    private Double radiusKm;
    private String cropFilter;
    private Integer count = 0;
    private String notice;
    private List<MandiPriceDto> results = new ArrayList<>();

    public static class LocationInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private Double latitude;
        private Double longitude;
        private String resolvedLocation;

        public LocationInfo() {}

        public LocationInfo(Double latitude, Double longitude, String resolvedLocation) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.resolvedLocation = resolvedLocation;
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public String getResolvedLocation() { return resolvedLocation; }
        public void setResolvedLocation(String resolvedLocation) { this.resolvedLocation = resolvedLocation; }
    }

    public MandiNearbyResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocationInfo getLocation() { return location; }
    public void setLocation(LocationInfo location) { this.location = location; }

    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }

    public String getCropFilter() { return cropFilter; }
    public void setCropFilter(String cropFilter) { this.cropFilter = cropFilter; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }

    public List<MandiPriceDto> getResults() { return results; }
    public void setResults(List<MandiPriceDto> results) {
        this.results = results != null ? results : new ArrayList<>();
        this.count = this.results.size();
    }
}

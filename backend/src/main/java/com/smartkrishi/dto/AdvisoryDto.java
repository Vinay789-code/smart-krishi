package com.smartkrishi.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AdvisoryDto {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category; // SCHEME, FERTILIZER, PEST_CONTROL, FARMING_TIPS, SEASONAL

    private String season;
    private String targetCrop;
    private String summary;

    @NotBlank(message = "Details content is required")
    private String details;

    private String applicableState;
    private String officialLink;
    private LocalDateTime createdAt;

    public AdvisoryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getTargetCrop() { return targetCrop; }
    public void setTargetCrop(String targetCrop) { this.targetCrop = targetCrop; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getApplicableState() { return applicableState; }
    public void setApplicableState(String applicableState) { this.applicableState = applicableState; }

    public String getOfficialLink() { return officialLink; }
    public void setOfficialLink(String officialLink) { this.officialLink = officialLink; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

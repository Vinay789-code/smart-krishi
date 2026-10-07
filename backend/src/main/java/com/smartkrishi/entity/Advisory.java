package com.smartkrishi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advisories")
public class Advisory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 50)
    private String category; // SCHEME, FERTILIZER, PEST_CONTROL, FARMING_TIPS, SEASONAL

    @Column(length = 50)
    private String season; // Kharif, Rabi, Zaid, All Season

    @Column(name = "target_crop", length = 100)
    private String targetCrop;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String details;

    @Column(name = "applicable_state", length = 100)
    private String applicableState = "All India";

    @Column(name = "official_link", length = 500)
    private String officialLink;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Advisory() {}

    public Advisory(String title, String category, String season, String targetCrop, String summary,
                    String details, String applicableState, String officialLink) {
        this.title = title;
        this.category = category;
        this.season = season;
        this.targetCrop = targetCrop;
        this.summary = summary;
        this.details = details;
        this.applicableState = applicableState != null ? applicableState : "All India";
        this.officialLink = officialLink;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getTargetCrop() {
        return targetCrop;
    }

    public void setTargetCrop(String targetCrop) {
        this.targetCrop = targetCrop;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getApplicableState() {
        return applicableState;
    }

    public void setApplicableState(String applicableState) {
        this.applicableState = applicableState;
    }

    public String getOfficialLink() {
        return officialLink;
    }

    public void setOfficialLink(String officialLink) {
        this.officialLink = officialLink;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

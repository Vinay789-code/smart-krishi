package com.smartkrishi.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiseaseAnalysisResponse {
    private Long recordId;
    private String cropName;
    private String imageName;
    private String imageUrl;
    private String detectedDisease;
    private Double confidenceScore;
    private String status; // "Disease Detected", "Healthy Leaf – No Disease Detected", "Low Confidence Diagnosis"
    private boolean lowConfidence;
    private String lowConfidenceMessage;
    private String severity; // LOW, MODERATE, HIGH
    private String symptoms;
    private String organicTreatment;
    private String chemicalTreatment;
    private String preventionTips;
    private List<AlternativePrediction> alternatives = new ArrayList<>();
    private LocalDateTime analyzedAt;

    public static class AlternativePrediction {
        private String disease;
        private Double confidence;

        public AlternativePrediction() {}

        public AlternativePrediction(String disease, Double confidence) {
            this.disease = disease;
            this.confidence = confidence;
        }

        public String getDisease() { return disease; }
        public void setDisease(String disease) { this.disease = disease; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }

    public DiseaseAnalysisResponse() {
        this.analyzedAt = LocalDateTime.now();
        this.status = "Disease Detected";
        this.lowConfidence = false;
    }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDetectedDisease() { return detectedDisease; }
    public void setDetectedDisease(String detectedDisease) { this.detectedDisease = detectedDisease; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isLowConfidence() { return lowConfidence; }
    public void setLowConfidence(boolean lowConfidence) { this.lowConfidence = lowConfidence; }

    public String getLowConfidenceMessage() { return lowConfidenceMessage; }
    public void setLowConfidenceMessage(String lowConfidenceMessage) { this.lowConfidenceMessage = lowConfidenceMessage; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getOrganicTreatment() { return organicTreatment; }
    public void setOrganicTreatment(String organicTreatment) { this.organicTreatment = organicTreatment; }

    public String getChemicalTreatment() { return chemicalTreatment; }
    public void setChemicalTreatment(String chemicalTreatment) { this.chemicalTreatment = chemicalTreatment; }

    public String getPreventionTips() { return preventionTips; }
    public void setPreventionTips(String preventionTips) { this.preventionTips = preventionTips; }

    public List<AlternativePrediction> getAlternatives() { return alternatives; }
    public void setAlternatives(List<AlternativePrediction> alternatives) { this.alternatives = alternatives; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}

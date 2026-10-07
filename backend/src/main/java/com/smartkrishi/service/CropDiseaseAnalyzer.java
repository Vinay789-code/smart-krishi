package com.smartkrishi.service;

import com.smartkrishi.dto.DiseaseAnalysisResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CropDiseaseAnalyzer {
    DiseaseAnalysisResponse analyze(MultipartFile file, String cropHint, Long userId);
}

package com.smartkrishi.repository;

import com.smartkrishi.entity.CropRecommendationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropRecommendationRepository extends JpaRepository<CropRecommendationRecord, Long> {
    List<CropRecommendationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CropRecommendationRecord> findTop5ByOrderByCreatedAtDesc();
}

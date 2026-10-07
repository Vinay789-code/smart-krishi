package com.smartkrishi.repository;

import com.smartkrishi.entity.DiseaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseRecordRepository extends JpaRepository<DiseaseRecord, Long> {
    List<DiseaseRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<DiseaseRecord> findTop5ByOrderByCreatedAtDesc();
}

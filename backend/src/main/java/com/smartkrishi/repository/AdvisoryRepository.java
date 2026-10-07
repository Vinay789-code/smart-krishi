package com.smartkrishi.repository;

import com.smartkrishi.entity.Advisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {
    List<Advisory> findByCategoryIgnoreCase(String category);
    List<Advisory> findBySeasonIgnoreCase(String season);
    List<Advisory> findTop4ByOrderByCreatedAtDesc();
}

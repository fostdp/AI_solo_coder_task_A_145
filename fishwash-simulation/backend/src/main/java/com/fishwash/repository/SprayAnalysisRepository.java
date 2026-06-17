package com.fishwash.repository;

import com.fishwash.entity.SprayAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprayAnalysisRepository extends JpaRepository<SprayAnalysis, Long> {

    Page<SprayAnalysis> findByDeviceIdOrderByAnalyzedAtDesc(Integer deviceId, Pageable pageable);
}

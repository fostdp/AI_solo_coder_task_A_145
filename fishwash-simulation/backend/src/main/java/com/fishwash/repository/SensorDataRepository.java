package com.fishwash.repository;

import com.fishwash.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Page<SensorData> findByDeviceIdOrderByRecordedAtDesc(Integer deviceId, Pageable pageable);

    List<SensorData> findByDeviceIdAndRecordedAtBetween(Integer deviceId, LocalDateTime start, LocalDateTime end);

    Optional<SensorData> findTop1ByDeviceIdOrderByRecordedAtDesc(Integer deviceId);
}

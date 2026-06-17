package com.fishwash.repository;

import com.fishwash.entity.VibrationMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VibrationModeRepository extends JpaRepository<VibrationMode, Long> {

    List<VibrationMode> findByDeviceIdOrderByModeOrderAsc(Integer deviceId);

    Optional<VibrationMode> findByDeviceIdAndModeOrder(Integer deviceId, Integer modeOrder);
}

package com.fishwash.repository;

import com.fishwash.entity.FishWashDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FishWashDeviceRepository extends JpaRepository<FishWashDevice, Integer> {

    Optional<FishWashDevice> findByDeviceCode(String deviceCode);

    List<FishWashDevice> findByStatus(String status);
}

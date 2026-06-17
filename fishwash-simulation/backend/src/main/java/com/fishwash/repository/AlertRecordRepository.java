package com.fishwash.repository;

import com.fishwash.entity.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findByIsResolvedFalse();

    List<AlertRecord> findByDeviceIdAndIsResolvedFalse(Integer deviceId);

    List<AlertRecord> findByAlertTypeAndIsResolvedFalse(String alertType);
}

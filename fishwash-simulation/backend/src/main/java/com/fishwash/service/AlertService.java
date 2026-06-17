package com.fishwash.service;

import com.fishwash.entity.AlertRecord;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.repository.AlertRecordRepository;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.service.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRecordRepository alertRecordRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final WebSocketNotifier webSocketNotifier;

    public List<AlertRecord> checkAlerts(Integer deviceId, Double frictionFreq, Double sprayHeight) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        List<AlertRecord> newAlerts = new ArrayList<>();

        if (device.getBaselineResonanceFreq() != null && frictionFreq != null) {
            double freqDrift = Math.abs(frictionFreq - device.getBaselineResonanceFreq())
                    / device.getBaselineResonanceFreq();
            if (freqDrift > 0.05) {
                AlertRecord alert = new AlertRecord();
                alert.setDeviceId(deviceId);
                alert.setAlertType("RESONANCE_DRIFT");
                alert.setAlertLevel(freqDrift > 0.15 ? "CRITICAL" : "WARNING");
                alert.setAlertMessage(String.format("Resonance frequency drift %.1f%% detected", freqDrift * 100));
                alert.setMetricValue(frictionFreq);
                alert.setThresholdValue(device.getBaselineResonanceFreq());
                alert.setIsResolved(false);
                alert.setCreatedAt(LocalDateTime.now());
                alert = alertRecordRepository.save(alert);
                webSocketNotifier.notifyAlert(alert);
                newAlerts.add(alert);
            }
        }

        if (device.getBaselineSprayHeight() != null && sprayHeight != null
                && device.getBaselineSprayHeight() > 0) {
            double sprayDeviation = Math.abs(sprayHeight - device.getBaselineSprayHeight())
                    / device.getBaselineSprayHeight();
            if (sprayDeviation > 0.3) {
                AlertRecord alert = new AlertRecord();
                alert.setDeviceId(deviceId);
                alert.setAlertType("SPRAY_ABNORMAL");
                alert.setAlertLevel(sprayDeviation > 0.5 ? "CRITICAL" : "WARNING");
                alert.setAlertMessage(String.format("Spray height deviation %.1f%% detected", sprayDeviation * 100));
                alert.setMetricValue(sprayHeight);
                alert.setThresholdValue(device.getBaselineSprayHeight());
                alert.setIsResolved(false);
                alert.setCreatedAt(LocalDateTime.now());
                alert = alertRecordRepository.save(alert);
                webSocketNotifier.notifyAlert(alert);
                newAlerts.add(alert);
            }
        }

        return newAlerts;
    }

    public List<AlertRecord> getActiveAlerts() {
        return alertRecordRepository.findByIsResolvedFalse();
    }

    public List<AlertRecord> getActiveAlertsByDevice(Integer deviceId) {
        return alertRecordRepository.findByDeviceIdAndIsResolvedFalse(deviceId);
    }

    public AlertRecord resolveAlert(Long alertId) {
        AlertRecord alert = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setIsResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return alertRecordRepository.save(alert);
    }
}

package com.fishwash.alarm_ws;

import com.fishwash.config.FishWashProperties;
import com.fishwash.entity.AlertRecord;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.event.SensorDataIngestedEvent;
import com.fishwash.event.SprayAnalysisCompletedEvent;
import com.fishwash.repository.AlertRecordRepository;
import com.fishwash.repository.FishWashDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmEvaluatorService {

    private final AlertRecordRepository alertRecordRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final FishWashProperties fishWashProperties;
    private final WebSocketNotifier webSocketNotifier;

    public List<AlertRecord> evaluateResonanceDrift(Integer deviceId, Double frictionFreq) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        List<AlertRecord> newAlerts = new ArrayList<>();

        if (device.getBaselineResonanceFreq() == null || frictionFreq == null) {
            return newAlerts;
        }

        double baseline = device.getBaselineResonanceFreq();
        double drift = Math.abs(frictionFreq - baseline) / baseline;
        FishWashProperties.AlertProps alertProps = fishWashProperties.getAlert();

        if (drift > alertProps.getResonanceDriftWarning()) {
            AlertRecord alert = new AlertRecord();
            alert.setDeviceId(deviceId);
            alert.setAlertType("RESONANCE_DRIFT");
            alert.setAlertLevel(drift > alertProps.getResonanceDriftCritical() ? "CRITICAL" : "WARNING");
            alert.setAlertMessage(String.format("Resonance frequency drift %.1f%% detected", drift * 100));
            alert.setMetricValue(frictionFreq);
            alert.setThresholdValue(baseline);
            alert.setIsResolved(false);
            alert.setCreatedAt(LocalDateTime.now());
            alert = alertRecordRepository.save(alert);
            webSocketNotifier.notifyAlert(alert);
            newAlerts.add(alert);
        }

        return newAlerts;
    }

    public List<AlertRecord> evaluateSprayDeviation(Integer deviceId, Double sprayHeight) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        List<AlertRecord> newAlerts = new ArrayList<>();

        if (device.getBaselineSprayHeight() == null || sprayHeight == null
                || device.getBaselineSprayHeight() <= 0) {
            return newAlerts;
        }

        double baseline = device.getBaselineSprayHeight();
        double deviation = Math.abs(sprayHeight - baseline) / baseline;
        FishWashProperties.AlertProps alertProps = fishWashProperties.getAlert();

        if (deviation > alertProps.getSprayDeviationWarning()) {
            AlertRecord alert = new AlertRecord();
            alert.setDeviceId(deviceId);
            alert.setAlertType("SPRAY_ABNORMAL");
            alert.setAlertLevel(deviation > alertProps.getSprayDeviationCritical() ? "CRITICAL" : "WARNING");
            alert.setAlertMessage(String.format("Spray height deviation %.1f%% detected", deviation * 100));
            alert.setMetricValue(sprayHeight);
            alert.setThresholdValue(baseline);
            alert.setIsResolved(false);
            alert.setCreatedAt(LocalDateTime.now());
            alert = alertRecordRepository.save(alert);
            webSocketNotifier.notifyAlert(alert);
            newAlerts.add(alert);
        }

        return newAlerts;
    }

    @EventListener
    public void onSensorDataIngested(SensorDataIngestedEvent event) {
        evaluateResonanceDrift(event.getDeviceId(), event.getFrictionFreq());
        evaluateSprayDeviation(event.getDeviceId(), event.getSprayHeight());
    }

    @EventListener
    public void onSprayAnalysisCompleted(SprayAnalysisCompletedEvent event) {
        FishWashProperties.AlertProps alertProps = fishWashProperties.getAlert();
        if (event.getDeviationRatio() > alertProps.getSprayDeviationWarning()) {
            AlertRecord alert = new AlertRecord();
            alert.setDeviceId(event.getDeviceId());
            alert.setAlertType("SPRAY_ABNORMAL");
            alert.setAlertLevel(event.getDeviationRatio() > alertProps.getSprayDeviationCritical() ? "CRITICAL" : "WARNING");
            alert.setAlertMessage(String.format("Spray analysis deviation %.1f%% detected (predicted: %.2f, actual: %.2f)",
                    event.getDeviationRatio() * 100, event.getPredictedSprayHeight(), event.getActualSprayHeight()));
            alert.setMetricValue(event.getActualSprayHeight());
            alert.setThresholdValue(event.getPredictedSprayHeight());
            alert.setIsResolved(false);
            alert.setCreatedAt(LocalDateTime.now());
            alert = alertRecordRepository.save(alert);
            webSocketNotifier.notifyAlert(alert);
        }
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

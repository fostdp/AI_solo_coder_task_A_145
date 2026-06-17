package com.fishwash.service;

import com.fishwash.entity.SensorData;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final SprayHeightService sprayHeightService;
    private final AlertService alertService;

    public SensorData ingestSensorData(Integer deviceId, Double frictionFreq, Double amplitude,
                                       Double sprayHeight, Double waterTemp, LocalDateTime recordedAt) {
        fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        SensorData data = new SensorData();
        data.setDeviceId(deviceId);
        data.setFrictionFreq(frictionFreq);
        data.setAmplitude(amplitude);
        data.setSprayHeight(sprayHeight);
        data.setWaterTemp(waterTemp);
        data.setRecordedAt(recordedAt);
        data = sensorDataRepository.save(data);

        final Double freq = frictionFreq;
        final Double height = sprayHeight;
        final Integer did = deviceId;
        CompletableFuture.runAsync(() -> {
            try {
                sprayHeightService.analyzeSprayHeight(did, freq, height);
            } catch (Exception ignored) {
            }
        });
        CompletableFuture.runAsync(() -> {
            try {
                alertService.checkAlerts(did, freq, height);
            } catch (Exception ignored) {
            }
        });

        return data;
    }

    public SensorData getLatestSensorData(Integer deviceId) {
        return sensorDataRepository.findTop1ByDeviceIdOrderByRecordedAtDesc(deviceId)
                .orElseThrow(() -> new RuntimeException("No sensor data found for device " + deviceId));
    }

    public List<SensorData> getSensorDataHistory(Integer deviceId, LocalDateTime start, LocalDateTime end) {
        return sensorDataRepository.findByDeviceIdAndRecordedAtBetween(deviceId, start, end);
    }

    public Page<SensorData> getSensorDataPage(Integer deviceId, int page, int size) {
        return sensorDataRepository.findByDeviceIdOrderByRecordedAtDesc(deviceId, PageRequest.of(page, size));
    }
}

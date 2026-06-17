package com.fishwash.dtu_receiver;

import com.fishwash.entity.SensorData;
import com.fishwash.event.SensorDataIngestedEvent;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DtuReceiverService {

    private final SensorDataRepository sensorDataRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SensorData ingestAndPublish(Integer deviceId, Double frictionFreq, Double amplitude,
                                       Double sprayHeight, Double waterTemp, LocalDateTime recordedAt) {
        fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        if (frictionFreq == null || frictionFreq <= 0 || frictionFreq >= 10000) {
            throw new IllegalArgumentException("frictionFreq must be > 0 and < 10000");
        }
        if (amplitude == null || amplitude < 0) {
            throw new IllegalArgumentException("amplitude must be >= 0");
        }
        if (sprayHeight == null || sprayHeight < 0) {
            throw new IllegalArgumentException("sprayHeight must be >= 0");
        }
        if (waterTemp == null || waterTemp < -10 || waterTemp > 100) {
            throw new IllegalArgumentException("waterTemp must be between -10 and 100");
        }

        SensorData data = new SensorData();
        data.setDeviceId(deviceId);
        data.setFrictionFreq(frictionFreq);
        data.setAmplitude(amplitude);
        data.setSprayHeight(sprayHeight);
        data.setWaterTemp(waterTemp);
        data.setRecordedAt(recordedAt);
        data = sensorDataRepository.save(data);

        eventPublisher.publishEvent(new SensorDataIngestedEvent(
                this, deviceId, frictionFreq, amplitude, sprayHeight, waterTemp, recordedAt));

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

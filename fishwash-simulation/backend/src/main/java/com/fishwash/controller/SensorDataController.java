package com.fishwash.controller;

import com.fishwash.dto.ApiResponse;
import com.fishwash.dto.SensorDataRequest;
import com.fishwash.entity.SensorData;
import com.fishwash.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataService sensorDataService;

    @PostMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<SensorData>> ingestSensorData(
            @PathVariable Integer deviceId,
            @RequestBody SensorDataRequest request) {
        SensorData data = sensorDataService.ingestSensorData(
                deviceId, request.getFrictionFreq(), request.getAmplitude(),
                request.getSprayHeight(), request.getWaterTemp(), request.getRecordedAt());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{deviceId}/latest")
    public ResponseEntity<ApiResponse<SensorData>> getLatestSensorData(
            @PathVariable Integer deviceId) {
        SensorData data = sensorDataService.getLatestSensorData(deviceId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{deviceId}/history")
    public ResponseEntity<ApiResponse<List<SensorData>>> getHistoryByTimeRange(
            @PathVariable Integer deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<SensorData> data = sensorDataService.getSensorDataHistory(deviceId, start, end);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{deviceId}/page")
    public ResponseEntity<ApiResponse<Page<SensorData>>> getPaginatedData(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SensorData> data = sensorDataService.getSensorDataPage(deviceId, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

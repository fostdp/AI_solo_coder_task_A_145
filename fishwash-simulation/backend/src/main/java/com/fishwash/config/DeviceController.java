package com.fishwash.config;

import com.fishwash.dto.ApiResponse;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.repository.FishWashDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final FishWashDeviceRepository fishWashDeviceRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FishWashDevice>>> listAllDevices() {
        List<FishWashDevice> devices = fishWashDeviceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<FishWashDevice>> getDeviceById(
            @PathVariable Integer deviceId) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    @GetMapping("/code/{deviceCode}")
    public ResponseEntity<ApiResponse<FishWashDevice>> getDeviceByCode(
            @PathVariable String deviceCode) {
        FishWashDevice device = fishWashDeviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return ResponseEntity.ok(ApiResponse.success(device));
    }
}

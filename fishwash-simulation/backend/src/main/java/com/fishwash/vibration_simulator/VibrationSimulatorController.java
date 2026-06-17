package com.fishwash.vibration_simulator;

import com.fishwash.dto.ApiResponse;
import com.fishwash.entity.VibrationMode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class VibrationSimulatorController {

    private final VibrationSimulatorService vibrationSimulatorService;

    @PostMapping("/vibration-mode/{deviceId}")
    public ResponseEntity<ApiResponse<VibrationMode>> calculateVibrationMode(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "2") int modeOrder) {
        VibrationMode mode = vibrationSimulatorService.calculateResonanceFrequency(deviceId, modeOrder);
        return ResponseEntity.ok(ApiResponse.success(mode));
    }

    @PostMapping("/vibration-mode/{deviceId}/shape")
    public ResponseEntity<ApiResponse<String>> calculateModeShape(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "2") int modeOrder,
            @RequestParam(defaultValue = "360") int resolution) {
        String shape = vibrationSimulatorService.calculateModeShape(deviceId, modeOrder, resolution);
        return ResponseEntity.ok(ApiResponse.success(shape));
    }

    @PostMapping("/modal-analysis/{deviceId}")
    public ResponseEntity<ApiResponse<List<VibrationMode>>> runModalAnalysis(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "6") int maxModeOrder) {
        List<VibrationMode> modes = vibrationSimulatorService.runModalAnalysis(deviceId, maxModeOrder);
        return ResponseEntity.ok(ApiResponse.success(modes));
    }

    @GetMapping("/vibration-modes/{deviceId}")
    public ResponseEntity<ApiResponse<List<VibrationMode>>> getVibrationModes(
            @PathVariable Integer deviceId) {
        List<VibrationMode> modes = vibrationSimulatorService.getVibrationModes(deviceId);
        return ResponseEntity.ok(ApiResponse.success(modes));
    }
}

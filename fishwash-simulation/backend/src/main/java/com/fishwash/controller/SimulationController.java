package com.fishwash.controller;

import com.fishwash.dto.ApiResponse;
import com.fishwash.dto.SprayAnalysisRequest;
import com.fishwash.entity.SprayAnalysis;
import com.fishwash.entity.VibrationMode;
import com.fishwash.service.SprayHeightService;
import com.fishwash.service.VibrationModalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final VibrationModalService vibrationModalService;
    private final SprayHeightService sprayHeightService;

    @PostMapping("/vibration-mode/{deviceId}")
    public ResponseEntity<ApiResponse<VibrationMode>> calculateVibrationMode(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "2") int modeOrder) {
        VibrationMode mode = vibrationModalService.calculateResonanceFrequency(deviceId, modeOrder);
        return ResponseEntity.ok(ApiResponse.success(mode));
    }

    @PostMapping("/vibration-mode/{deviceId}/shape")
    public ResponseEntity<ApiResponse<String>> calculateModeShape(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "2") int modeOrder,
            @RequestParam(defaultValue = "360") int resolution) {
        String shape = vibrationModalService.calculateModeShape(deviceId, modeOrder, resolution);
        return ResponseEntity.ok(ApiResponse.success(shape));
    }

    @PostMapping("/modal-analysis/{deviceId}")
    public ResponseEntity<ApiResponse<List<VibrationMode>>> runModalAnalysis(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "6") int maxModeOrder) {
        List<VibrationMode> modes = vibrationModalService.runModalAnalysis(deviceId, maxModeOrder);
        return ResponseEntity.ok(ApiResponse.success(modes));
    }

    @GetMapping("/vibration-modes/{deviceId}")
    public ResponseEntity<ApiResponse<List<VibrationMode>>> getVibrationModes(
            @PathVariable Integer deviceId) {
        List<VibrationMode> modes = vibrationModalService.getVibrationModes(deviceId);
        return ResponseEntity.ok(ApiResponse.success(modes));
    }

    @PostMapping("/spray-analysis/{deviceId}")
    public ResponseEntity<ApiResponse<SprayAnalysis>> analyzeSpray(
            @PathVariable Integer deviceId,
            @RequestBody SprayAnalysisRequest request) {
        SprayAnalysis analysis = sprayHeightService.analyzeSprayHeight(
                deviceId, request.getFrictionFreq(), request.getMeasuredSprayHeight());
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @GetMapping("/spray-analysis/{deviceId}/history")
    public ResponseEntity<ApiResponse<Page<SprayAnalysis>>> getSprayAnalysisHistory(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SprayAnalysis> analyses = sprayHeightService.getSprayAnalysisHistory(deviceId, page, size);
        return ResponseEntity.ok(ApiResponse.success(analyses));
    }
}

package com.fishwash.water_jet_analyzer;

import com.fishwash.dto.ApiResponse;
import com.fishwash.dto.SprayAnalysisRequest;
import com.fishwash.entity.SprayAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spray")
@RequiredArgsConstructor
public class WaterJetAnalyzerController {

    private final WaterJetAnalyzerService waterJetAnalyzerService;

    @PostMapping("/analysis/{deviceId}")
    public ResponseEntity<ApiResponse<SprayAnalysis>> analyzeSprayHeight(
            @PathVariable Integer deviceId,
            @RequestBody SprayAnalysisRequest request) {
        SprayAnalysis analysis = waterJetAnalyzerService.analyzeSprayHeight(
                deviceId, request.getFrictionFreq(), request.getMeasuredSprayHeight());
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @GetMapping("/analysis/{deviceId}/history")
    public ResponseEntity<ApiResponse<Page<SprayAnalysis>>> getSprayAnalysisHistory(
            @PathVariable Integer deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SprayAnalysis> history = waterJetAnalyzerService.getSprayAnalysisHistory(deviceId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}

package com.fishwash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.entity.SprayAnalysis;
import com.fishwash.entity.VibrationMode;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.repository.SprayAnalysisRepository;
import com.fishwash.repository.VibrationModeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SprayHeightService {

    private static final double GRAVITY = 9.81;
    private static final double WATER_DENSITY = 1000.0;

    private final SprayAnalysisRepository sprayAnalysisRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final VibrationModeRepository vibrationModeRepository;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public SprayAnalysis analyzeSprayHeight(Integer deviceId, Double frictionFreq, Double measuredSprayHeight) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        try {
            Map<String, Object> materialParams = objectMapper.readValue(device.getMaterialParams(), Map.class);
            Map<String, Object> geometryParams = objectMapper.readValue(device.getGeometryParams(), Map.class);

            double density = ((Number) materialParams.get("density")).doubleValue();
            double thickness = ((Number) materialParams.get("thickness")).doubleValue();
            double radius = ((Number) geometryParams.get("radius")).doubleValue();
            double waterDepth = ((Number) geometryParams.get("height")).doubleValue() * 0.7;

            List<VibrationMode> modes = vibrationModeRepository.findByDeviceIdOrderByModeOrderAsc(deviceId);

            VibrationMode closestMode = null;
            double minFreqDiff = Double.MAX_VALUE;
            for (VibrationMode mode : modes) {
                double diff = Math.abs(mode.getResonanceFreq() - frictionFreq);
                if (diff < minFreqDiff) {
                    minFreqDiff = diff;
                    closestMode = mode;
                }
            }

            int modeOrder = closestMode != null ? closestMode.getModeOrder() : 2;
            double resonanceFreq = closestMode != null ? closestMode.getResonanceFreq() : frictionFreq;

            double kCoupling = 1.0 / Math.sqrt(1.0 + WATER_DENSITY * radius / (density * thickness * modeOrder));

            double efficiency;
            if (waterDepth < 0.05) {
                efficiency = 0.3;
            } else if (waterDepth < 0.1) {
                efficiency = 0.4;
            } else {
                efficiency = 0.5;
            }

            double amplitude = 0.001;
            double omega = 2.0 * Math.PI * resonanceFreq;
            double predictedHeight = (omega * omega * amplitude * amplitude) / (2.0 * GRAVITY) * kCoupling * efficiency;

            double deviationRatio = measuredSprayHeight != null && measuredSprayHeight > 0
                    ? Math.abs(predictedHeight - measuredSprayHeight) / measuredSprayHeight
                    : 0.0;

            SprayAnalysis analysis = new SprayAnalysis();
            analysis.setDeviceId(deviceId);
            analysis.setFrictionFreq(frictionFreq);
            analysis.setPredictedSprayHeight(predictedHeight);
            analysis.setActualSprayHeight(measuredSprayHeight);
            analysis.setStandingWaveNodes(modeOrder * 2);
            analysis.setSplashModelParams(String.format(
                    "{\"kCoupling\":%.6f,\"efficiency\":%.2f,\"waterDepth\":%.4f,\"modeOrder\":%d}",
                    kCoupling, efficiency, waterDepth, modeOrder));
            analysis.setDeviationRatio(deviationRatio);
            analysis.setAnalyzedAt(LocalDateTime.now());
            analysis = sprayAnalysisRepository.save(analysis);

            if (deviationRatio > 0.3) {
                alertService.checkAlerts(deviceId, frictionFreq, measuredSprayHeight);
            }

            return analysis;
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze spray height", e);
        }
    }

    public Page<SprayAnalysis> getSprayAnalysisHistory(Integer deviceId, int page, int size) {
        return sprayAnalysisRepository.findByDeviceIdOrderByAnalyzedAtDesc(deviceId, PageRequest.of(page, size));
    }
}

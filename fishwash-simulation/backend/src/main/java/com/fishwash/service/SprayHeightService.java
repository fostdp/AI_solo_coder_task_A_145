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
    private static final double ALE_SURFACE_STABILITY = 0.92;
    private static final double SECONDARY_BREAKUP_THRESHOLD = 2.5;
    private static final double SPLASH_COEFFICIENT = 0.65;

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

            double aleCouplingFactor;
            if (closestMode != null && closestMode.getFluidCouplingFactor() != null) {
                aleCouplingFactor = closestMode.getFluidCouplingFactor();
            } else {
                double rawCoupling = 1.0 / Math.sqrt(1.0 + WATER_DENSITY * radius / (density * thickness * modeOrder));
                aleCouplingFactor = rawCoupling * ALE_SURFACE_STABILITY;
            }

            double efficiency = calculateSprayEfficiency(waterDepth, modeOrder, radius);

            double amplitude = estimateAmplitude(frictionFreq, resonanceFreq, modeOrder, radius);

            double omega = 2.0 * Math.PI * resonanceFreq;
            double baseHeight = (omega * omega * amplitude * amplitude) / (2.0 * GRAVITY);

            double aleStabilityFactor = calculateAleSurfaceStability(modeOrder, waterDepth, radius);
            double splashAmplification = calculateSplashAmplification(modeOrder, amplitude, waterDepth);

            double predictedHeightCm = baseHeight * aleCouplingFactor * efficiency
                    * aleStabilityFactor * splashAmplification * 100.0;

            double deviationRatio = measuredSprayHeight != null && measuredSprayHeight > 0
                    ? Math.abs(predictedHeightCm - measuredSprayHeight) / measuredSprayHeight
                    : 0.0;

            int secondaryBreakupCount = estimateSecondaryBreakupParticles(modeOrder, amplitude, waterDepth);

            String splashParams = String.format(
                    "{\"aleCouplingFactor\":%.6f,\"efficiency\":%.4f,\"waterDepth\":%.4f," +
                            "\"modeOrder\":%d,\"aleStabilityFactor\":%.4f,\"splashAmplification\":%.4f," +
                            "\"baseAmplitude\":%.6f,\"secondaryBreakupParticles\":%d," +
                            "\"splashCoefficient\":%.2f,\"aleSurfaceStability\":%.3f}",
                    aleCouplingFactor, efficiency, waterDepth,
                    modeOrder, aleStabilityFactor, splashAmplification,
                    amplitude, secondaryBreakupCount,
                    SPLASH_COEFFICIENT, ALE_SURFACE_STABILITY);

            SprayAnalysis analysis = new SprayAnalysis();
            analysis.setDeviceId(deviceId);
            analysis.setFrictionFreq(frictionFreq);
            analysis.setPredictedSprayHeight(predictedHeightCm);
            analysis.setActualSprayHeight(measuredSprayHeight);
            analysis.setStandingWaveNodes(modeOrder * 2);
            analysis.setSplashModelParams(splashParams);
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

    private double calculateSprayEfficiency(double waterDepth, int modeOrder, double radius) {
        double depthRatio = waterDepth / radius;
        double baseEfficiency;
        if (depthRatio < 0.2) {
            baseEfficiency = 0.3;
        } else if (depthRatio < 0.4) {
            baseEfficiency = 0.4;
        } else {
            baseEfficiency = 0.5;
        }
        double modeFactor = 1.0 - (modeOrder - 2) * 0.05;
        return baseEfficiency * Math.max(0.5, modeFactor);
    }

    private double estimateAmplitude(double frictionFreq, double resonanceFreq, int modeOrder, double radius) {
        double freqRatio = frictionFreq / resonanceFreq;
        double damping = 0.01 + 0.005 * modeOrder;
        double denom = Math.sqrt((1.0 - freqRatio * freqRatio) * (1.0 - freqRatio * freqRatio)
                + (2.0 * damping * freqRatio) * (2.0 * damping * freqRatio));
        if (denom < 1e-6) denom = 1e-6;
        double dynamicAmpFactor = 1.0 / denom;
        double baseAmp = radius * 0.002;
        return baseAmp * dynamicAmpFactor;
    }

    private double calculateAleSurfaceStability(int modeOrder, double waterDepth, double radius) {
        double curvatureRatio = waterDepth / radius;
        double modeFactor = 1.0 / Math.sqrt(1.0 + modeOrder * 0.15);
        double stability = ALE_SURFACE_STABILITY + (1.0 - curvatureRatio) * 0.05;
        return Math.min(1.0, stability * modeFactor);
    }

    private double calculateSplashAmplification(int modeOrder, double amplitude, double waterDepth) {
        double weberNumber = WATER_DENSITY * amplitude * amplitude * modeOrder * modeOrder / 0.073;
        if (weberNumber > SECONDARY_BREAKUP_THRESHOLD) {
            return 1.0 + SPLASH_COEFFICIENT * Math.log10(weberNumber / SECONDARY_BREAKUP_THRESHOLD + 1.0);
        }
        return 1.0;
    }

    private int estimateSecondaryBreakupParticles(int modeOrder, double amplitude, double waterDepth) {
        int baseParticles = modeOrder * 2 * 20;
        double weberNumber = WATER_DENSITY * amplitude * amplitude * modeOrder * modeOrder / 0.073;
        if (weberNumber > SECONDARY_BREAKUP_THRESHOLD) {
            double multiplier = 1.0 + (weberNumber - SECONDARY_BREAKUP_THRESHOLD) * 0.3;
            baseParticles = (int) (baseParticles * Math.min(5.0, multiplier));
        }
        return baseParticles;
    }

    public Page<SprayAnalysis> getSprayAnalysisHistory(Integer deviceId, int page, int size) {
        return sprayAnalysisRepository.findByDeviceIdOrderByAnalyzedAtDesc(deviceId, PageRequest.of(page, size));
    }
}

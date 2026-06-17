package com.fishwash.water_jet_analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishwash.config.FishWashProperties;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.entity.SensorData;
import com.fishwash.entity.SprayAnalysis;
import com.fishwash.entity.VibrationMode;
import com.fishwash.event.SensorDataIngestedEvent;
import com.fishwash.event.SprayAnalysisCompletedEvent;
import com.fishwash.event.VibrationModeComputedEvent;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.repository.SensorDataRepository;
import com.fishwash.repository.SprayAnalysisRepository;
import com.fishwash.repository.VibrationModeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaterJetAnalyzerService {

    private final SprayAnalysisRepository sprayAnalysisRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final VibrationModeRepository vibrationModeRepository;
    private final SensorDataRepository sensorDataRepository;
    private final FishWashProperties fishWashProperties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @SuppressWarnings("unchecked")
    public SprayAnalysis analyzeSprayHeight(Integer deviceId, Double frictionFreq, Double measuredSprayHeight) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        double waterDensity = fishWashProperties.getFluid().getWaterDensity();
        double surfaceTension = fishWashProperties.getFluid().getSurfaceTension();
        double gravity = fishWashProperties.getFluid().getGravity();
        double aleSurfaceStability = fishWashProperties.getAle().getSurfaceStability();
        double secondaryBreakupThreshold = fishWashProperties.getSplash().getSecondaryBreakupThreshold();
        double splashCoefficient = fishWashProperties.getSplash().getCoefficient();

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
                double rawCoupling = 1.0 / Math.sqrt(1.0 + waterDensity * radius / (density * thickness * modeOrder));
                aleCouplingFactor = rawCoupling * aleSurfaceStability;
            }

            double efficiency = calculateSprayEfficiency(waterDepth, modeOrder, radius);

            double amplitude = estimateAmplitude(frictionFreq, resonanceFreq, modeOrder, radius);

            double omega = 2.0 * Math.PI * resonanceFreq;
            double baseHeight = (omega * omega * amplitude * amplitude) / (2.0 * gravity);

            double aleStabilityFactor = calculateAleSurfaceStability(modeOrder, waterDepth, radius, aleSurfaceStability);
            double splashAmplification = calculateSplashAmplification(modeOrder, amplitude, waterDensity, surfaceTension, secondaryBreakupThreshold, splashCoefficient);

            double predictedHeightCm = baseHeight * aleCouplingFactor * efficiency
                    * aleStabilityFactor * splashAmplification * 100.0;

            double deviationRatio = measuredSprayHeight != null && measuredSprayHeight > 0
                    ? Math.abs(predictedHeightCm - measuredSprayHeight) / measuredSprayHeight
                    : 0.0;

            int secondaryBreakupCount = estimateSecondaryBreakupParticles(modeOrder, amplitude, waterDensity, surfaceTension, secondaryBreakupThreshold);

            String splashParams = String.format(
                    "{\"aleCouplingFactor\":%.6f,\"efficiency\":%.4f,\"waterDepth\":%.4f," +
                            "\"modeOrder\":%d,\"aleStabilityFactor\":%.4f,\"splashAmplification\":%.4f," +
                            "\"baseAmplitude\":%.6f,\"secondaryBreakupParticles\":%d," +
                            "\"splashCoefficient\":%.2f,\"aleSurfaceStability\":%.3f}",
                    aleCouplingFactor, efficiency, waterDepth,
                    modeOrder, aleStabilityFactor, splashAmplification,
                    amplitude, secondaryBreakupCount,
                    splashCoefficient, aleSurfaceStability);

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

            eventPublisher.publishEvent(new SprayAnalysisCompletedEvent(
                    this, deviceId, frictionFreq, predictedHeightCm, measuredSprayHeight, deviationRatio));

            return analysis;
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze spray height", e);
        }
    }

    @EventListener
    public void onSensorDataIngested(SensorDataIngestedEvent event) {
        analyzeSprayHeight(event.getDeviceId(), event.getFrictionFreq(), event.getSprayHeight());
    }

    @EventListener
    public void onVibrationModeComputed(VibrationModeComputedEvent event) {
        SensorData latestData = sensorDataRepository
                .findTop1ByDeviceIdOrderByRecordedAtDesc(event.getDeviceId())
                .orElse(null);
        if (latestData != null) {
            analyzeSprayHeight(event.getDeviceId(), latestData.getFrictionFreq(), latestData.getSprayHeight());
        }
    }

    public Page<SprayAnalysis> getSprayAnalysisHistory(Integer deviceId, int page, int size) {
        return sprayAnalysisRepository.findByDeviceIdOrderByAnalyzedAtDesc(deviceId, PageRequest.of(page, size));
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

    private double calculateAleSurfaceStability(int modeOrder, double waterDepth, double radius, double aleSurfaceStability) {
        double curvatureRatio = waterDepth / radius;
        double modeFactor = 1.0 / Math.sqrt(1.0 + modeOrder * 0.15);
        double stability = aleSurfaceStability + (1.0 - curvatureRatio) * 0.05;
        return Math.min(1.0, stability * modeFactor);
    }

    private double calculateSplashAmplification(int modeOrder, double amplitude, double waterDensity, double surfaceTension, double secondaryBreakupThreshold, double splashCoefficient) {
        double weberNumber = waterDensity * amplitude * amplitude * modeOrder * modeOrder / surfaceTension;
        if (weberNumber > secondaryBreakupThreshold) {
            return 1.0 + splashCoefficient * Math.log10(weberNumber / secondaryBreakupThreshold + 1.0);
        }
        return 1.0;
    }

    private int estimateSecondaryBreakupParticles(int modeOrder, double amplitude, double waterDensity, double surfaceTension, double secondaryBreakupThreshold) {
        int baseParticles = modeOrder * 2 * 20;
        double weberNumber = waterDensity * amplitude * amplitude * modeOrder * modeOrder / surfaceTension;
        if (weberNumber > secondaryBreakupThreshold) {
            double multiplier = 1.0 + (weberNumber - secondaryBreakupThreshold) * 0.3;
            baseParticles = (int) (baseParticles * Math.min(5.0, multiplier));
        }
        return baseParticles;
    }
}

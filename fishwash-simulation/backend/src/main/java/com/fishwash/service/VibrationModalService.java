package com.fishwash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishwash.entity.FishWashDevice;
import com.fishwash.entity.VibrationMode;
import com.fishwash.repository.FishWashDeviceRepository;
import com.fishwash.repository.VibrationModeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VibrationModalService {

    private static final double WATER_DENSITY = 1000.0;
    private static final double ALE_TRANSITION_RATIO = 0.15;
    private static final double MESH_DISTORTION_THRESHOLD = 0.35;
    private static final double ARTIFICIAL_VISCOSITY = 0.02;
    private static final double ALE_STABILITY_FACTOR = 0.85;

    private final VibrationModeRepository vibrationModeRepository;
    private final FishWashDeviceRepository fishWashDeviceRepository;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public VibrationMode calculateResonanceFrequency(Integer deviceId, int modeOrder) {
        FishWashDevice device = fishWashDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        try {
            Map<String, Object> materialParams = objectMapper.readValue(device.getMaterialParams(), Map.class);
            Map<String, Object> geometryParams = objectMapper.readValue(device.getGeometryParams(), Map.class);

            double density = ((Number) materialParams.get("density")).doubleValue();
            double elasticModulus = ((Number) materialParams.get("elasticModulus")).doubleValue();
            double poissonRatio = ((Number) materialParams.get("poissonRatio")).doubleValue();
            double thickness = ((Number) materialParams.get("thickness")).doubleValue();
            double radius = ((Number) geometryParams.get("radius")).doubleValue();
            double waterDepth = ((Number) geometryParams.get("height")).doubleValue() * 0.7;

            double D = elasticModulus * Math.pow(thickness, 3) / (12.0 * (1.0 - poissonRatio * poissonRatio));

            double lambdaN = Math.pow(modeOrder, 2) * (Math.pow(modeOrder, 2) - 1);

            double fDry = (lambdaN / (2.0 * Math.PI)) * Math.sqrt(D / (density * thickness * radius * radius));

            double rawFluidCoupling = 1.0 / Math.sqrt(
                    1.0 + WATER_DENSITY * radius / (density * thickness * modeOrder));

            double aleCorrectedCoupling = calculateAleCorrectedCoupling(
                    rawFluidCoupling, modeOrder, waterDepth, radius, density, thickness);

            double fWet = fDry * aleCorrectedCoupling;

            double aleStabilityMargin = calculateAleStabilityMargin(modeOrder, waterDepth, radius);

            double dampingRatio = 0.01 + 0.005 * modeOrder + ARTIFICIAL_VISCOSITY * aleStabilityMargin;

            VibrationMode mode = new VibrationMode();
            mode.setDeviceId(deviceId);
            mode.setModeOrder(modeOrder);
            mode.setResonanceFreq(fWet);
            mode.setDampingRatio(dampingRatio);
            mode.setFluidCouplingFactor(aleCorrectedCoupling);
            mode.setCalculatedAt(LocalDateTime.now());
            mode = vibrationModeRepository.save(mode);

            return mode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate resonance frequency", e);
        }
    }

    private double calculateAleCorrectedCoupling(double rawCoupling, int modeOrder,
                                                  double waterDepth, double radius,
                                                  double shellDensity, double shellThickness) {
        double aleLayerThickness = waterDepth * ALE_TRANSITION_RATIO;

        double addedMassFactor = WATER_DENSITY * radius / (shellDensity * shellThickness * modeOrder);

        double aleTransitionFactor = 1.0 - ALE_TRANSITION_RATIO * 0.5;

        double meshDistortion = estimateMeshDistortion(modeOrder, waterDepth, radius);

        double correction = 1.0;
        if (meshDistortion > MESH_DISTORTION_THRESHOLD * 0.5) {
            double excessDistortion = (meshDistortion - MESH_DISTORTION_THRESHOLD * 0.5)
                    / (MESH_DISTORTION_THRESHOLD * 0.5);
            correction = 1.0 + excessDistortion * ALE_STABILITY_FACTOR * 0.15;
        }

        double correctedFactor = rawCoupling * aleTransitionFactor * correction;

        double lowerBound = 1.0 / Math.sqrt(1.0 + addedMassFactor * 1.5);
        return Math.max(lowerBound, correctedFactor);
    }

    private double estimateMeshDistortion(int modeOrder, double waterDepth, double radius) {
        double surfaceAmplitudeFactor = modeOrder * modeOrder * 0.02;
        double curvatureFactor = 1.0 / Math.sqrt(1.0 + waterDepth / radius);
        return surfaceAmplitudeFactor * curvatureFactor;
    }

    private double calculateAleStabilityMargin(int modeOrder, double waterDepth, double radius) {
        double baseStability = 1.0;
        double modePenalty = (modeOrder - 2) * 0.08;
        double depthFactor = Math.min(1.0, waterDepth / (radius * 0.5));
        return baseStability - modePenalty * depthFactor;
    }

    public String calculateModeShape(Integer deviceId, int modeOrder, int resolution) {
        StringBuilder json = new StringBuilder("[");

        int circumferentialElements = resolution;
        int axialElements = 20;
        int aleLayerElements = (int) (axialElements * ALE_TRANSITION_RATIO * 2);
        int totalElements = circumferentialElements * axialElements;
        int totalNodes = (circumferentialElements + 1) * (axialElements + 1);

        int remeshingCycles = calculateRemeshingCycles(modeOrder, resolution);

        double[] aleMeshVelocities = calculateAleMeshVelocities(modeOrder, axialElements);

        for (int i = 0; i < resolution; i++) {
            double angle = 2.0 * Math.PI * i / resolution;

            double surfaceDisplacement = Math.cos(modeOrder * angle);

            double aleSmoothedDisplacement = applyAleSmoothing(surfaceDisplacement, modeOrder, angle);

            if (i > 0) {
                json.append(",");
            }
            json.append(String.format(
                    "{\"angle\":%.6f,\"displacement\":%.6f,\"aleDisplacement\":%.6f,\"meshVelocity\":%.6f}",
                    angle, surfaceDisplacement, aleSmoothedDisplacement,
                    aleMeshVelocities[i % aleMeshVelocities.length]));
        }
        json.append("]");

        String aleParams = String.format(
                "\"aleTransitionRatio\":%.4f,\"meshDistortionThreshold\":%.4f," +
                        "\"artificialViscosity\":%.4f,\"aleStabilityFactor\":%.4f," +
                        "\"aleLayerElements\":%d,\"remeshingCycles\":%d",
                ALE_TRANSITION_RATIO, MESH_DISTORTION_THRESHOLD,
                ARTIFICIAL_VISCOSITY, ALE_STABILITY_FACTOR,
                aleLayerElements, remeshingCycles);

        String femMeshInfo = String.format(
                "{\"circumferentialElements\":%d,\"axialElements\":%d,\"totalElements\":%d," +
                        "\"totalNodes\":%d,\"aleMethod\":\"arbitrary-lagrangian-eulerian\",%s}",
                circumferentialElements, axialElements, totalElements, totalNodes, aleParams);

        vibrationModeRepository.findByDeviceIdAndModeOrder(deviceId, modeOrder).ifPresent(mode -> {
            mode.setModeShape(json.toString());
            mode.setFemMeshInfo(femMeshInfo);
            vibrationModeRepository.save(mode);
        });

        return json.toString();
    }

    private double[] calculateAleMeshVelocities(int modeOrder, int axialElements) {
        double[] velocities = new double[360];
        for (int i = 0; i < 360; i++) {
            double angle = 2.0 * Math.PI * i / 360;
            double surfaceVelocity = -modeOrder * Math.sin(modeOrder * angle);
            velocities[i] = surfaceVelocity * ALE_STABILITY_FACTOR;
        }
        return velocities;
    }

    private double applyAleSmoothing(double displacement, int modeOrder, double angle) {
        double smoothingWidth = 2.0 * Math.PI / (modeOrder * 8.0);
        double smoothed = 0.0;
        double weightSum = 0.0;
        for (int k = -3; k <= 3; k++) {
            double da = k * smoothingWidth;
            double w = Math.exp(-da * da / (2.0 * smoothingWidth * smoothingWidth));
            double a = angle + da;
            smoothed += Math.cos(modeOrder * a) * w;
            weightSum += w;
        }
        return smoothed / weightSum;
    }

    private int calculateRemeshingCycles(int modeOrder, int resolution) {
        int baseCycles = 1;
        if (modeOrder > 3) {
            baseCycles += (modeOrder - 3) * 2;
        }
        if (resolution > 180) {
            baseCycles += 1;
        }
        return baseCycles;
    }

    public List<VibrationMode> runModalAnalysis(Integer deviceId, int maxModeOrder) {
        List<VibrationMode> modes = new ArrayList<>();
        for (int n = 2; n <= maxModeOrder; n++) {
            VibrationMode mode = calculateResonanceFrequency(deviceId, n);
            calculateModeShape(deviceId, n, 360);
            modes.add(mode);
        }
        return modes;
    }

    public List<VibrationMode> getVibrationModes(Integer deviceId) {
        return vibrationModeRepository.findByDeviceIdOrderByModeOrderAsc(deviceId);
    }
}

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

            double D = elasticModulus * Math.pow(thickness, 3) / (12.0 * (1.0 - poissonRatio * poissonRatio));

            double lambdaN = Math.pow(modeOrder, 2) * (Math.pow(modeOrder, 2) - 1);

            double fDry = (lambdaN / (2.0 * Math.PI)) * Math.sqrt(D / (density * thickness * radius * radius));

            double fluidCouplingFactor = 1.0 / Math.sqrt(
                    1.0 + WATER_DENSITY * radius / (density * thickness * modeOrder));
            double fWet = fDry * fluidCouplingFactor;

            VibrationMode mode = new VibrationMode();
            mode.setDeviceId(deviceId);
            mode.setModeOrder(modeOrder);
            mode.setResonanceFreq(fWet);
            mode.setDampingRatio(0.01 + 0.005 * modeOrder);
            mode.setFluidCouplingFactor(fluidCouplingFactor);
            mode.setCalculatedAt(LocalDateTime.now());
            mode = vibrationModeRepository.save(mode);

            return mode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate resonance frequency", e);
        }
    }

    public String calculateModeShape(Integer deviceId, int modeOrder, int resolution) {
        StringBuilder json = new StringBuilder("[");
        double axialModeFactor = 1.0;

        int circumferentialElements = resolution;
        int axialElements = 20;
        int totalElements = circumferentialElements * axialElements;
        int totalNodes = (circumferentialElements + 1) * (axialElements + 1);

        for (int i = 0; i < resolution; i++) {
            double angle = 2.0 * Math.PI * i / resolution;
            double displacement = Math.cos(modeOrder * angle) * axialModeFactor;
            if (i > 0) {
                json.append(",");
            }
            json.append(String.format("{\"angle\":%.6f,\"displacement\":%.6f}", angle, displacement));
        }
        json.append("]");

        String femMeshInfo = String.format(
                "{\"circumferentialElements\":%d,\"axialElements\":%d,\"totalElements\":%d,\"totalNodes\":%d}",
                circumferentialElements, axialElements, totalElements, totalNodes);

        vibrationModeRepository.findByDeviceIdAndModeOrder(deviceId, modeOrder).ifPresent(mode -> {
            mode.setModeShape(json.toString());
            mode.setFemMeshInfo(femMeshInfo);
            vibrationModeRepository.save(mode);
        });

        return json.toString();
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

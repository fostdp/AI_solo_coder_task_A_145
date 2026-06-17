package com.fishwash.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "fishwash")
@Data
public class FishWashProperties {

    private MaterialProps material = new MaterialProps();
    private FluidProps fluid = new FluidProps();
    private AleProps ale = new AleProps();
    private SplashProps splash = new SplashProps();
    private AlertProps alert = new AlertProps();

    @Data
    public static class MaterialProps {
        private Map<String, MaterialProfile> profiles;
    }

    @Data
    public static class MaterialProfile {
        private double density;
        private double elasticModulus;
        private double poissonRatio;
        private double tinContentPct;
    }

    @Data
    public static class FluidProps {
        private double waterDensity = 1000.0;
        private double surfaceTension = 0.073;
        private double gravity = 9.81;
    }

    @Data
    public static class AleProps {
        private double transitionRatio = 0.15;
        private double meshDistortionThreshold = 0.35;
        private double artificialViscosity = 0.02;
        private double stabilityFactor = 0.85;
        private double surfaceStability = 0.92;
    }

    @Data
    public static class SplashProps {
        private double secondaryBreakupThreshold = 3.5;
        private double coefficient = 0.65;
    }

    @Data
    public static class AlertProps {
        private double resonanceDriftWarning = 0.05;
        private double resonanceDriftCritical = 0.15;
        private double sprayDeviationWarning = 0.30;
        private double sprayDeviationCritical = 0.50;
    }
}

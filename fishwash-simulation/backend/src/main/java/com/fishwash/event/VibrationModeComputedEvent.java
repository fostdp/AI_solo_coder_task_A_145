package com.fishwash.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class VibrationModeComputedEvent extends ApplicationEvent {

    private Integer deviceId;
    private int modeOrder;
    private double resonanceFreq;
    private double fluidCouplingFactor;
    private double dampingRatio;

    public VibrationModeComputedEvent(Object source, Integer deviceId, int modeOrder,
                                      double resonanceFreq, double fluidCouplingFactor,
                                      double dampingRatio) {
        super(source);
        this.deviceId = deviceId;
        this.modeOrder = modeOrder;
        this.resonanceFreq = resonanceFreq;
        this.fluidCouplingFactor = fluidCouplingFactor;
        this.dampingRatio = dampingRatio;
    }
}

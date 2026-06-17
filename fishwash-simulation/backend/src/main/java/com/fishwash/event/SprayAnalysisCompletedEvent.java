package com.fishwash.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class SprayAnalysisCompletedEvent extends ApplicationEvent {

    private Integer deviceId;
    private Double frictionFreq;
    private Double predictedSprayHeight;
    private Double actualSprayHeight;
    private Double deviationRatio;

    public SprayAnalysisCompletedEvent(Object source, Integer deviceId, Double frictionFreq,
                                       Double predictedSprayHeight, Double actualSprayHeight,
                                       Double deviationRatio) {
        super(source);
        this.deviceId = deviceId;
        this.frictionFreq = frictionFreq;
        this.predictedSprayHeight = predictedSprayHeight;
        this.actualSprayHeight = actualSprayHeight;
        this.deviationRatio = deviationRatio;
    }
}

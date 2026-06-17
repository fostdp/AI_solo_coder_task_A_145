package com.fishwash.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
@Setter
public class SensorDataIngestedEvent extends ApplicationEvent {

    private Integer deviceId;
    private Double frictionFreq;
    private Double amplitude;
    private Double sprayHeight;
    private Double waterTemp;
    private LocalDateTime recordedAt;

    public SensorDataIngestedEvent(Object source, Integer deviceId, Double frictionFreq,
                                   Double amplitude, Double sprayHeight, Double waterTemp,
                                   LocalDateTime recordedAt) {
        super(source);
        this.deviceId = deviceId;
        this.frictionFreq = frictionFreq;
        this.amplitude = amplitude;
        this.sprayHeight = sprayHeight;
        this.waterTemp = waterTemp;
        this.recordedAt = recordedAt;
    }
}

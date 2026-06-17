package com.fishwash.dto;

import java.time.LocalDateTime;

public class SensorDataRequest {

    private Double frictionFreq;
    private Double amplitude;
    private Double sprayHeight;
    private Double waterTemp;
    private LocalDateTime recordedAt;

    public Double getFrictionFreq() {
        return frictionFreq;
    }

    public void setFrictionFreq(Double frictionFreq) {
        this.frictionFreq = frictionFreq;
    }

    public Double getAmplitude() {
        return amplitude;
    }

    public void setAmplitude(Double amplitude) {
        this.amplitude = amplitude;
    }

    public Double getSprayHeight() {
        return sprayHeight;
    }

    public void setSprayHeight(Double sprayHeight) {
        this.sprayHeight = sprayHeight;
    }

    public Double getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(Double waterTemp) {
        this.waterTemp = waterTemp;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}

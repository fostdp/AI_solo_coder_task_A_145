package com.fishwash.dto;

public class SprayAnalysisRequest {

    private Double frictionFreq;
    private Double measuredSprayHeight;

    public Double getFrictionFreq() {
        return frictionFreq;
    }

    public void setFrictionFreq(Double frictionFreq) {
        this.frictionFreq = frictionFreq;
    }

    public Double getMeasuredSprayHeight() {
        return measuredSprayHeight;
    }

    public void setMeasuredSprayHeight(Double measuredSprayHeight) {
        this.measuredSprayHeight = measuredSprayHeight;
    }
}

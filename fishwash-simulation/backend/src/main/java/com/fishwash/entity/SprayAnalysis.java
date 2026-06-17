package com.fishwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "spray_analysis")
public class SprayAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "friction_freq")
    private Double frictionFreq;

    @Column(name = "predicted_spray_height")
    private Double predictedSprayHeight;

    @Column(name = "actual_spray_height")
    private Double actualSprayHeight;

    @Column(name = "standing_wave_nodes")
    private Integer standingWaveNodes;

    @Column(name = "splash_model_params", columnDefinition = "jsonb")
    private String splashModelParams;

    @Column(name = "deviation_ratio")
    private Double deviationRatio;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}

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
@Table(name = "vibration_mode")
public class VibrationMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "mode_order", nullable = false)
    private Integer modeOrder;

    @Column(name = "resonance_freq")
    private Double resonanceFreq;

    @Column(name = "mode_shape", columnDefinition = "jsonb")
    private String modeShape;

    @Column(name = "damping_ratio")
    private Double dampingRatio;

    @Column(name = "fem_mesh_info", columnDefinition = "jsonb")
    private String femMeshInfo;

    @Column(name = "fluid_coupling_factor")
    private Double fluidCouplingFactor;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}

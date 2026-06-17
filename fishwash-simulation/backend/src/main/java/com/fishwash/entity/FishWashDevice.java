package com.fishwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fishwash_device")
public class FishWashDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "device_code", nullable = false, unique = true)
    private String deviceCode;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "era")
    private String era;

    @Column(name = "material_params", columnDefinition = "jsonb")
    private String materialParams;

    @Column(name = "geometry_params", columnDefinition = "jsonb")
    private String geometryParams;

    @Column(name = "baseline_resonance_freq")
    private Double baselineResonanceFreq;

    @Column(name = "baseline_spray_height")
    private Double baselineSprayHeight;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.fishwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sensor_data")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "friction_freq")
    private Double frictionFreq;

    @Column(name = "amplitude")
    private Double amplitude;

    @Column(name = "spray_height")
    private Double sprayHeight;

    @Column(name = "water_temp")
    private Double waterTemp;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;

    @PrePersist
    protected void onPersist() {
        if (this.ingestedAt == null) {
            this.ingestedAt = LocalDateTime.now();
        }
    }
}

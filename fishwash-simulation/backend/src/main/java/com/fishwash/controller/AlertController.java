package com.fishwash.controller;

import com.fishwash.dto.ApiResponse;
import com.fishwash.entity.AlertRecord;
import com.fishwash.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getActiveAlerts() {
        List<AlertRecord> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/active/{deviceId}")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getActiveAlertsForDevice(
            @PathVariable Integer deviceId) {
        List<AlertRecord> alerts = alertService.getActiveAlertsByDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<ApiResponse<AlertRecord>> resolveAlert(
            @PathVariable Long alertId) {
        AlertRecord alert = alertService.resolveAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }
}

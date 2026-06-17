package com.fishwash.alarm_ws;

import com.fishwash.dto.ApiResponse;
import com.fishwash.entity.AlertRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmEvaluatorService alarmEvaluatorService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getActiveAlerts() {
        List<AlertRecord> alerts = alarmEvaluatorService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/active/{deviceId}")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getActiveAlertsByDevice(
            @PathVariable Integer deviceId) {
        List<AlertRecord> alerts = alarmEvaluatorService.getActiveAlertsByDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<ApiResponse<AlertRecord>> resolveAlert(
            @PathVariable Long alertId) {
        AlertRecord alert = alarmEvaluatorService.resolveAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }
}

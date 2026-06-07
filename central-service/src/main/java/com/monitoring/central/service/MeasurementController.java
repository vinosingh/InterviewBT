package com.monitoring.central.service;

import com.monitoring.central.alarm.AlarmService;
import com.monitoring.central.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the Central Monitoring Service API.
 *
 * Endpoint design decisions:
 *
 * POST /api/measurements (single)
 *   - Simple, standard REST.
 *   - Returns 202 Accepted immediately; alarm evaluation is asynchronous.
 *
 * POST /api/measurements/batch (multiple)
 *   - Allows a warehouse service to publish a burst of buffered readings in one call.
 *   - Reduces TCP/HTTP overhead when sensors send at high frequency.
 *
 * GET /api/status
 *   - Health/status endpoint so operators can verify the service is alive.
 */
@RestController
@RequestMapping("/api")
public class MeasurementController {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementController.class);

    private final AlarmService alarmService;

    public MeasurementController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    /**
     * Accepts a single sensor reading.
     * Returns 202 Accepted — the reading has been queued for evaluation.
     */
    @PostMapping("/measurements")
    public ResponseEntity<Map<String, String>> receiveMeasurement(
            @RequestBody SensorReading reading) {

        LOG.info("Received measurement from warehouse={} sensor={} type={} value={}",
                reading.getWarehouseId(), reading.getSensorId(),
                reading.getType(), reading.getValue());

        alarmService.evaluate(reading);

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "sensorId", reading.getSensorId()));
    }

    /**
     * Accepts a batch of sensor readings in a single HTTP call.
     * Each reading is independently evaluated.
     */
    @PostMapping("/measurements/batch")
    public ResponseEntity<Map<String, Object>> receiveBatch(
            @RequestBody List<SensorReading> readings) {

        LOG.info("Received batch of {} measurements", readings.size());

        readings.forEach(alarmService::evaluate);

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "count", readings.size()));
    }

    /**
     * Simple liveness + stats endpoint.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "Central Monitoring Service",
                "status", "UP",
                "alarmsRaised", alarmService.getAlarmCount()
        ));
    }
}

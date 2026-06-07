package com.monitoring.central.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Immutable value object representing a single sensor reading received
 * from a warehouse service.
 *
 * Design notes:
 * - Immutable (final fields, no setters) — safe to share across threads.
 * - Uses Java record-style constructor with @JsonCreator for explicit deserialization.
 * - warehouseId links the reading back to its source warehouse.
 * - receivedAt is stamped by the central service on arrival (not the sensor clock)
 *   to avoid clock-skew issues across distributed nodes.
 */
public class SensorReading {

    private final String sensorId;
    private final SensorType type;
    private final double value;
    private final String warehouseId;
    private final Instant receivedAt;

    @JsonCreator
    public SensorReading(
            @JsonProperty("sensorId") String sensorId,
            @JsonProperty("type") SensorType type,
            @JsonProperty("value") double value,
            @JsonProperty("warehouseId") String warehouseId) {
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.warehouseId = warehouseId;
        this.receivedAt = Instant.now();
    }

    public String getSensorId() { return sensorId; }
    public SensorType getType() { return type; }
    public double getValue() { return value; }
    public String getWarehouseId() { return warehouseId; }
    public Instant getReceivedAt() { return receivedAt; }

    @Override
    public String toString() {
        return "SensorReading{sensorId='%s', type=%s, value=%.2f, warehouseId='%s', receivedAt=%s}"
                .formatted(sensorId, type, value, warehouseId, receivedAt);
    }
}

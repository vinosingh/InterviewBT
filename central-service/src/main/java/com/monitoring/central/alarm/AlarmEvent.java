package com.monitoring.central.alarm;

import com.monitoring.central.model.SensorReading;
import com.monitoring.central.model.SensorType;

import java.time.Instant;

/**
 * Represents a threshold-breach alarm event.
 *
 * Using a Java 17 record here because:
 * - It is a pure data carrier with no behaviour.
 * - Records give us equals/hashCode/toString for free.
 * - Immutability by default — records cannot be mutated after creation.
 *
 * This is the payload published to the alarm channel / logging system.
 */
public record AlarmEvent(
        String warehouseId,
        String sensorId,
        SensorType sensorType,
        double measuredValue,
        double threshold,
        Instant triggeredAt
) {
    /**
     * Factory method to create an AlarmEvent from a SensorReading and its threshold.
     */
    public static AlarmEvent of(SensorReading reading, double threshold) {
        return new AlarmEvent(
                reading.getWarehouseId(),
                reading.getSensorId(),
                reading.getType(),
                reading.getValue(),
                threshold,
                Instant.now()
        );
    }

    /**
     * Human-readable alarm description for log output.
     */
    public String toAlarmMessage() {
        String unit = sensorType == com.monitoring.central.model.SensorType.TEMPERATURE ? "°C" : "%";
        return """
                ⚠️  ALARM TRIGGERED ⚠️
                  Warehouse  : %s
                  Sensor     : %s (%s)
                  Measured   : %.2f%s
                  Threshold  : %.2f%s
                  Exceeded By: %.2f%s
                  Triggered  : %s
                """.formatted(
                warehouseId, sensorId, sensorType,
                measuredValue, unit,
                threshold, unit,
                measuredValue - threshold, unit,
                triggeredAt
        );
    }
}

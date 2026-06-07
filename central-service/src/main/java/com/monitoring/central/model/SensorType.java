package com.monitoring.central.model;

/**
 * Discriminated union of supported sensor types.
 * Adding a new sensor type here is the single place to extend the system
 * — the alarm service uses this enum to dispatch threshold checks.
 */
public enum SensorType {
    TEMPERATURE,
    HUMIDITY
}

package com.monitoring.warehouse.model;

/**
 * Data Transfer Object sent from the Warehouse Service to the Central Service.
 *
 * This mirrors the SensorReading in the central-service module.
 * In a real multi-team project these would live in a shared "api-contracts" module
 * (published as a jar) so both services stay in sync without code duplication.
 * For this self-contained assignment they are replicated to keep modules independent.
 */
public class SensorReading {

    private String sensorId;
    private SensorType type;
    private double value;
    private String warehouseId;

    public SensorReading() {}

    public SensorReading(String sensorId, SensorType type, double value, String warehouseId) {
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.warehouseId = warehouseId;
    }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = type; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    @Override
    public String toString() {
        return "SensorReading{sensorId='%s', type=%s, value=%.2f, warehouseId='%s'}"
                .formatted(sensorId, type, value, warehouseId);
    }
}

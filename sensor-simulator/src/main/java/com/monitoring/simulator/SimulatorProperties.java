package com.monitoring.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the sensor simulator, bound from application.yml.
 */
@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {

    private String targetHost = "localhost";
    private SensorSimConfig temperature = new SensorSimConfig();
    private SensorSimConfig humidity = new SensorSimConfig();

    public static class SensorSimConfig {
        private int port;
        private String sensorId;
        private double baseValue;
        private double spikeValue;
        private int intervalSeconds;
        private int spikeEveryN;

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getSensorId() { return sensorId; }
        public void setSensorId(String sensorId) { this.sensorId = sensorId; }
        public double getBaseValue() { return baseValue; }
        public void setBaseValue(double baseValue) { this.baseValue = baseValue; }
        public double getSpikeValue() { return spikeValue; }
        public void setSpikeValue(double spikeValue) { this.spikeValue = spikeValue; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public int getSpikeEveryN() { return spikeEveryN; }
        public void setSpikeEveryN(int spikeEveryN) { this.spikeEveryN = spikeEveryN; }
    }

    public String getTargetHost() { return targetHost; }
    public void setTargetHost(String targetHost) { this.targetHost = targetHost; }
    public SensorSimConfig getTemperature() { return temperature; }
    public void setTemperature(SensorSimConfig temperature) { this.temperature = temperature; }
    public SensorSimConfig getHumidity() { return humidity; }
    public void setHumidity(SensorSimConfig humidity) { this.humidity = humidity; }
}

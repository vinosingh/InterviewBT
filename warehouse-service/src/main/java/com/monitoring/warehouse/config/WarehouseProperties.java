package com.monitoring.warehouse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the warehouse service.
 * Maps from application.yml under the "warehouse" prefix.
 */
@Component
@ConfigurationProperties(prefix = "warehouse")
public class WarehouseProperties {

    private String id = "WH-01";
    private Sensors sensors = new Sensors();
    private CentralService centralService = new CentralService();

    public static class Sensors {
        private SensorConfig temperature = new SensorConfig(3344);
        private SensorConfig humidity = new SensorConfig(3355);

        public SensorConfig getTemperature() { return temperature; }
        public void setTemperature(SensorConfig temperature) { this.temperature = temperature; }
        public SensorConfig getHumidity() { return humidity; }
        public void setHumidity(SensorConfig humidity) { this.humidity = humidity; }
    }

    public static class SensorConfig {
        private int udpPort;

        public SensorConfig() {}
        public SensorConfig(int udpPort) { this.udpPort = udpPort; }

        public int getUdpPort() { return udpPort; }
        public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    }

    public static class CentralService {
        private String url = "http://localhost:8080/api/measurements";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Sensors getSensors() { return sensors; }
    public void setSensors(Sensors sensors) { this.sensors = sensors; }
    public CentralService getCentralService() { return centralService; }
    public void setCentralService(CentralService centralService) { this.centralService = centralService; }
}

package com.monitoring.central.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds threshold values from application.yml into a strongly-typed bean.
 *
 * Using @ConfigurationProperties over @Value provides:
 * - Type-safe binding
 * - Grouped namespace (central-monitoring.thresholds.*)
 * - Easy testability — can inject mock configs in unit tests
 */
@Component
@ConfigurationProperties(prefix = "central-monitoring.thresholds")
public class ThresholdProperties {

    /** Maximum allowed temperature in Celsius */
    private double temperature = 35.0;

    /** Maximum allowed humidity percentage */
    private double humidity = 50.0;

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }
}

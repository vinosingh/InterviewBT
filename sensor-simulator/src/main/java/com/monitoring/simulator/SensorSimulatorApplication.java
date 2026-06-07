package com.monitoring.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sensor Simulator — replaces physical sensors for local testing.
 *
 * Sends UDP packets on the same ports that the warehouse service listens to.
 * Periodically sends "spike" values that exceed thresholds to trigger alarms.
 *
 * @EnableScheduling activates the @Scheduled annotation used in SensorSimulator.
 */
@SpringBootApplication
@EnableScheduling
public class SensorSimulatorApplication {

    public static void main(String[] args) {

        SpringApplication.run(SensorSimulatorApplication.class, args);
    }
}

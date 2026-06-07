package com.monitoring.central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Central Monitoring Service - Entry Point
 * <p>
 * This service acts as the hub that:
 * 1. Receives sensor measurements from all warehouse services via HTTP POST
 * 2. Evaluates each measurement against configured thresholds
 * 3. Fires an ALARM log if any threshold is breached
 *
 * @EnableAsync allows alarm processing to be non-blocking so the REST endpoint
 * returns quickly and alarms are evaluated on a separate thread pool.
 */
@SpringBootApplication
@EnableAsync
public class CentralMonitoringApplication {

    public static void main(String[] args) {

        SpringApplication.run(CentralMonitoringApplication.class, args);
    }
}

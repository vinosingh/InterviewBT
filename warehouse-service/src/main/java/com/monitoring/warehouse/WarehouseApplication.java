package com.monitoring.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Warehouse Service — Entry Point
 *
 * Starts two UDP listener threads (temperature on 3344, humidity on 3355)
 * and an HTTP forwarder that publishes parsed readings to the Central Service.
 */
@SpringBootApplication
@EnableAsync
public class WarehouseApplication {

    public static void main(String[] args) {

        SpringApplication.run(WarehouseApplication.class, args);
    }
}

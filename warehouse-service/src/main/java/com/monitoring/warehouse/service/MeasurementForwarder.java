package com.monitoring.warehouse.service;

import com.monitoring.warehouse.config.WarehouseProperties;
import com.monitoring.warehouse.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Forwards sensor readings from the Warehouse Service to the Central Service via HTTP POST.
 *
 * Why @Async here?
 * - The UDP receive loop runs on a dedicated thread. If the HTTP POST blocks
 *   (e.g., network latency), it would delay processing of the next UDP packet.
 * - @Async offloads the HTTP call to Spring's task executor, keeping the UDP
 *   listener thread always ready to accept the next datagram.
 *
 * Error handling:
 * - RestClientException is caught and logged. The reading is dropped on failure.
 * - Production improvement: add a retry with exponential backoff (Resilience4j)
 *   and a local dead-letter queue so readings aren't lost if central is temporarily down.
 */
@Service
public class MeasurementForwarder {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementForwarder.class);

    private final RestTemplate restTemplate;
    private final WarehouseProperties props;

    public MeasurementForwarder(RestTemplate restTemplate, WarehouseProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * Sends a sensor reading to the Central Monitoring Service.
     * Runs asynchronously on Spring's default task executor.
     *
     * @param reading the parsed and enriched sensor reading
     */
    @Async
    public void forward(SensorReading reading) {
        String url = props.getCentralService().getUrl();
        LOG.debug("Forwarding reading {} to {}", reading, url);

        try {
            restTemplate.postForEntity(url, reading, String.class);
            LOG.info("Successfully forwarded reading for sensor '{}' (value={}) to central service",
                    reading.getSensorId(), reading.getValue());
        } catch (RestClientException e) {
            LOG.error("Failed to forward reading for sensor '{}': {}. " +
                            "Central service may be unavailable.",
                    reading.getSensorId(), e.getMessage());
        }
    }
}

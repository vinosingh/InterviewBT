package com.monitoring.warehouse.udp;

import com.monitoring.warehouse.config.WarehouseProperties;
import com.monitoring.warehouse.model.SensorType;
import com.monitoring.warehouse.service.MeasurementForwarder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle of all UDP listener threads.
 *
 * Why a dedicated manager?
 * - UdpListener is a Runnable (not a Spring bean) so it can be unit-tested
 *   without Spring context.
 * - This manager is the Spring integration point: it wires dependencies,
 *   starts threads on @PostConstruct, and stops them on @PreDestroy.
 *
 * Thread model:
 * - Uses a virtual-thread executor (Java 21 preview) — downgraded to a
 *   fixed-thread-pool here for Java 17 compatibility.
 * - Two daemon threads: one per sensor type. Daemon threads don't prevent
 *   JVM shutdown, but we still stop them explicitly for clean socket release.
 */
@Component
public class UdpListenerManager {

    private static final Logger LOG = LoggerFactory.getLogger(UdpListenerManager.class);

    private final WarehouseProperties props;
    private final UdpMessageParser parser;
    private final MeasurementForwarder forwarder;

    private final List<UdpListener> listeners = new ArrayList<>();
    private ExecutorService executorService;

    public UdpListenerManager(WarehouseProperties props,
                               UdpMessageParser parser,
                               MeasurementForwarder forwarder) {
        this.props = props;
        this.parser = parser;
        this.forwarder = forwarder;
    }

    @PostConstruct
    public void startListeners() {
        // Named thread factory for observability in thread dumps
        executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("udp-listener-" + t.getId());
            return t;
        });

        UdpListener tempListener = new UdpListener(
                props.getSensors().getTemperature().getUdpPort(),
                SensorType.TEMPERATURE,
                props.getId(),
                parser,
                forwarder
        );

        UdpListener humidListener = new UdpListener(
                props.getSensors().getHumidity().getUdpPort(),
                SensorType.HUMIDITY,
                props.getId(),
                parser,
                forwarder
        );

        listeners.add(tempListener);
        listeners.add(humidListener);

        listeners.forEach(executorService::submit);

        LOG.info("Started {} UDP listeners for warehouse '{}'", listeners.size(), props.getId());
    }

    @PreDestroy
    public void stopListeners() {
        LOG.info("Shutting down UDP listeners...");
        listeners.forEach(UdpListener::stop);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("All UDP listeners stopped.");
    }
}

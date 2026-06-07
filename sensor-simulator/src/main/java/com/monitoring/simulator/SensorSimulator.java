package com.monitoring.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates temperature and humidity sensors by sending UDP datagrams
 * to the warehouse service at configured intervals.
 *
 * Spike mechanism:
 * Each sensor has an independent counter. Every Nth tick, the simulator
 * sends a spike value (above threshold) to trigger an alarm. This lets
 * you observe the end-to-end alarm flow without manual intervention.
 *
 * Wire format (matches spec):
 *   sensor_id=t1; value=30
 *   sensor_id=h1; value=40
 */
@Component
public class SensorSimulator {

    private static final Logger LOG = LoggerFactory.getLogger(SensorSimulator.class);

    private final SimulatorProperties props;

    private final AtomicInteger tempTickCount = new AtomicInteger(0);
    private final AtomicInteger humidTickCount = new AtomicInteger(0);

    public SensorSimulator(SimulatorProperties props) {
        this.props = props;
    }

    /**
     * Temperature sensor — sends a reading every N seconds.
     * fixedRateString reads the interval from config (in milliseconds).
     * We multiply by 1000 in YAML: interval-seconds → fixedDelay expression.
     */
    @Scheduled(fixedDelayString = "#{simulatorProperties.temperature.intervalSeconds * 1000}")
    public void sendTemperatureReading() {
        SimulatorProperties.SensorSimConfig cfg = props.getTemperature();
        int tick = tempTickCount.incrementAndGet();
        double value = (tick % cfg.getSpikeEveryN() == 0) ? cfg.getSpikeValue() : cfg.getBaseValue();
        sendUdpPacket(cfg.getSensorId(), value, cfg.getPort(), "TEMPERATURE");
    }

    /**
     * Humidity sensor — sends a reading every N seconds.
     */
    @Scheduled(fixedDelayString = "#{simulatorProperties.humidity.intervalSeconds * 1000}")
    public void sendHumidityReading() {
        SimulatorProperties.SensorSimConfig cfg = props.getHumidity();
        int tick = humidTickCount.incrementAndGet();
        double value = (tick % cfg.getSpikeEveryN() == 0) ? cfg.getSpikeValue() : cfg.getBaseValue();
        sendUdpPacket(cfg.getSensorId(), value, cfg.getPort(), "HUMIDITY");
    }

    /**
     * Sends a single UDP datagram in the spec-defined wire format.
     *
     * A new DatagramSocket is created per message (no connection state needed for UDP).
     * The socket is auto-closed via try-with-resources.
     */
    private void sendUdpPacket(String sensorId, double value, int port, String type) {
        String message = "sensor_id=%s; value=%.1f".formatted(sensorId, value);
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(props.getTargetHost());
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);

            boolean isSpike = value > (type.equals("TEMPERATURE") ?
                    props.getTemperature().getBaseValue() : props.getHumidity().getBaseValue());

            LOG.info("[{}] Sent UDP → {}:{} | {} {}",
                    type, props.getTargetHost(), port, message,
                    isSpike ? "⚡ SPIKE!" : "");

        } catch (Exception e) {
            LOG.error("[{}] Failed to send UDP packet to port {}: {}", type, port, e.getMessage());
        }
    }
}

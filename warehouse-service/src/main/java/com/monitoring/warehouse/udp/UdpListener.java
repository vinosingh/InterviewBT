package com.monitoring.warehouse.udp;

import com.monitoring.warehouse.model.SensorReading;
import com.monitoring.warehouse.model.SensorType;
import com.monitoring.warehouse.service.MeasurementForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * A Runnable UDP listener that blocks on a DatagramSocket, reads incoming packets,
 * parses them, and hands off the result to the MeasurementForwarder.
 *
 * Design decisions:
 *
 * 1. Runnable (not a Spring @Component):
 *    Instantiated by UdpListenerManager, which controls the thread lifecycle.
 *    This allows one listener class to serve both temperature and humidity ports
 *    simply by passing a different port and SensorType at construction time.
 *
 * 2. Buffer size = 1024 bytes:
 *    The spec's message format ("sensor_id=t1; value=30") is ~25 bytes.
 *    1024 is generous headroom for extended IDs / future fields.
 *
 * 3. Graceful shutdown via volatile `running` flag + socket.close():
 *    When the Spring context stops, UdpListenerManager calls stop(), which sets
 *    running=false and closes the socket. The blocking receive() call throws a
 *    SocketException, the catch block checks `running`, and the thread exits cleanly.
 */
public class UdpListener implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UdpListener.class);
    private static final int BUFFER_SIZE = 1024;

    private final int port;
    private final SensorType sensorType;
    private final String warehouseId;
    private final UdpMessageParser parser;
    private final MeasurementForwarder forwarder;

    private volatile boolean running = false;
    private DatagramSocket socket;

    public UdpListener(int port, SensorType sensorType, String warehouseId,
                       UdpMessageParser parser, MeasurementForwarder forwarder) {
        this.port = port;
        this.sensorType = sensorType;
        this.warehouseId = warehouseId;
        this.parser = parser;
        this.forwarder = forwarder;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            running = true;
            LOG.info("UDP listener started for {} sensor on port {}", sensorType, port);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);  // blocks until a datagram arrives

                String raw = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                LOG.debug("Received UDP packet on port {}: '{}'", port, raw);

                parser.parse(raw).ifPresentOrElse(
                        parsed -> {
                            SensorReading reading = new SensorReading(
                                    parsed.sensorId(), sensorType, parsed.value(), warehouseId);
                            LOG.info("Parsed sensor reading: {}", reading);
                            forwarder.forward(reading);
                        },
                        () -> LOG.warn("Could not parse UDP packet from port {}: '{}'", port, raw)
                );
            }

        } catch (java.net.SocketException e) {
            if (running) {
                LOG.error("UDP socket error on port {}: {}", port, e.getMessage());
            } else {
                LOG.info("UDP listener on port {} stopped gracefully.", port);
            }
        } catch (Exception e) {
            LOG.error("Fatal error in UDP listener on port {}: {}", port, e.getMessage(), e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LOG.info("UDP listener for {} on port {} shut down.", sensorType, port);
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();  // unblocks the receive() call
        }
    }

    public boolean isRunning() {
        return running;
    }
}

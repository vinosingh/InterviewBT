package com.monitoring.central.alarm;

import com.monitoring.central.config.ThresholdProperties;
import com.monitoring.central.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Core alarm evaluation engine.
 *
 * Responsibilities:
 *  1. Receive a SensorReading (delivered asynchronously from the REST layer).
 *  2. Look up the configured threshold for the sensor's type.
 *  3. If the measured value exceeds the threshold → create and log an AlarmEvent.
 *
 * Why @Async?
 *  The HTTP POST from the warehouse service is acknowledged immediately (202 Accepted).
 *  The threshold check runs on the dedicated "alarmExecutor" thread pool, keeping
 *  the web thread free for the next incoming batch of readings.
 *
 * Thread safety:
 *  alarmCount uses AtomicLong — safe for concurrent increment from multiple threads.
 */
@Service
public class AlarmService {

    // Separate logger so alarm messages can be routed to a dedicated appender
    // (e.g., separate alarm.log file) via Logback configuration.
    private static final Logger ALARM_LOGGER = LoggerFactory.getLogger("ALARM");
    private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

    private final ThresholdProperties thresholds;
    private final AtomicLong alarmCount = new AtomicLong(0);

    public AlarmService(ThresholdProperties thresholds) {
        this.thresholds = thresholds;
    }

    /**
     * Evaluates a sensor reading against its threshold.
     * Runs asynchronously on the alarmExecutor thread pool.
     *
     * @param reading the incoming sensor measurement
     */
    @Async("alarmExecutor")
    public void evaluate(SensorReading reading) {
        LOG.debug("Evaluating reading: {}", reading);

        double threshold = resolveThreshold(reading);

        if (reading.getValue() > threshold) {
            AlarmEvent alarm = AlarmEvent.of(reading, threshold);
            long count = alarmCount.incrementAndGet();
            ALARM_LOGGER.error("\n{}", alarm.toAlarmMessage());
            LOG.warn("Alarm #{} raised for warehouse={} sensor={} value={} threshold={}",
                    count, reading.getWarehouseId(), reading.getSensorId(),
                    reading.getValue(), threshold);
        } else {
            LOG.info("Reading OK — warehouse={} sensor={} value={} (threshold={})",
                    reading.getWarehouseId(), reading.getSensorId(),
                    reading.getValue(), threshold);
        }
    }

    /**
     * Dispatches threshold lookup by sensor type.
     * Switch expression (Java 14+) is exhaustive — compiler enforces all enum arms.
     */
    private double resolveThreshold(SensorReading reading) {
        return switch (reading.getType()) {
            case TEMPERATURE -> thresholds.getTemperature();
            case HUMIDITY -> thresholds.getHumidity();
        };
    }

    /** Exposed for testing and metrics */
    public long getAlarmCount() {
        return alarmCount.get();
    }
}

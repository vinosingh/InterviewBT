package com.monitoring.central;

import com.monitoring.central.alarm.AlarmService;
import com.monitoring.central.config.ThresholdProperties;
import com.monitoring.central.model.SensorReading;
import com.monitoring.central.model.SensorType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style tests for AlarmService using real Spring context.
 *
 * We use @SpringBootTest so that @Async wiring (the executor bean) is real.
 * Awaitility is used to wait for async alarm evaluation to complete before asserting.
 */
@SpringBootTest
@ActiveProfiles("test")
class AlarmServiceTest {

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private ThresholdProperties thresholds;

    @BeforeEach
    void verifyThresholds() {
        // Confirm test thresholds are loaded
        assertThat(thresholds.getTemperature()).isEqualTo(35.0);
        assertThat(thresholds.getHumidity()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Temperature reading below threshold should NOT raise alarm")
    void temperatureBelow_noAlarm() {
        long before = alarmService.getAlarmCount();
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 30.0, "WH-01");
        alarmService.evaluate(reading);

        // Give async task time to run, then assert count unchanged
        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before));
    }

    @Test
    @DisplayName("Temperature reading above threshold should raise alarm")
    void temperatureAbove_raisesAlarm() {
        long before = alarmService.getAlarmCount();
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 40.0, "WH-01");
        alarmService.evaluate(reading);

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before + 1));
    }

    @Test
    @DisplayName("Temperature reading exactly at threshold should NOT raise alarm")
    void temperatureAtThreshold_noAlarm() {
        long before = alarmService.getAlarmCount();
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 35.0, "WH-01");
        alarmService.evaluate(reading);

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before));
    }

    @Test
    @DisplayName("Humidity reading below threshold should NOT raise alarm")
    void humidityBelow_noAlarm() {
        long before = alarmService.getAlarmCount();
        SensorReading reading = new SensorReading("h1", SensorType.HUMIDITY, 45.0, "WH-02");
        alarmService.evaluate(reading);

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before));
    }

    @Test
    @DisplayName("Humidity reading above threshold should raise alarm")
    void humidityAbove_raisesAlarm() {
        long before = alarmService.getAlarmCount();
        SensorReading reading = new SensorReading("h1", SensorType.HUMIDITY, 75.0, "WH-02");
        alarmService.evaluate(reading);

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before + 1));
    }

    @Test
    @DisplayName("Multiple alarms from different warehouses should each increment count")
    void multipleAlarms_allCounted() {
        long before = alarmService.getAlarmCount();

        alarmService.evaluate(new SensorReading("t1", SensorType.TEMPERATURE, 40.0, "WH-01"));
        alarmService.evaluate(new SensorReading("h1", SensorType.HUMIDITY, 60.0, "WH-01"));
        alarmService.evaluate(new SensorReading("t2", SensorType.TEMPERATURE, 38.0, "WH-02"));

        Awaitility.await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(alarmService.getAlarmCount()).isEqualTo(before + 3));
    }
}

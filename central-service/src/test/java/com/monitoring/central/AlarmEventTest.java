package com.monitoring.central;

import com.monitoring.central.alarm.AlarmEvent;
import com.monitoring.central.model.SensorReading;
import com.monitoring.central.model.SensorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for the AlarmEvent record — no Spring context needed.
 */
class AlarmEventTest {

    @Test
    @DisplayName("AlarmEvent.of() captures reading fields correctly")
    void alarmEvent_capturesFields() {
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 42.5, "WH-03");
        AlarmEvent event = AlarmEvent.of(reading, 35.0);

        assertThat(event.warehouseId()).isEqualTo("WH-03");
        assertThat(event.sensorId()).isEqualTo("t1");
        assertThat(event.sensorType()).isEqualTo(SensorType.TEMPERATURE);
        assertThat(event.measuredValue()).isEqualTo(42.5);
        assertThat(event.threshold()).isEqualTo(35.0);
        assertThat(event.triggeredAt()).isNotNull();
    }

    @Test
    @DisplayName("toAlarmMessage() contains all relevant fields")
    void alarmMessage_containsAllFields() {
        SensorReading reading = new SensorReading("h1", SensorType.HUMIDITY, 65.0, "WH-01");
        AlarmEvent event = AlarmEvent.of(reading, 50.0);
        String msg = event.toAlarmMessage();

        assertThat(msg).contains("WH-01");
        assertThat(msg).contains("h1");
        assertThat(msg).contains("HUMIDITY");
        assertThat(msg).contains("65.00");
        assertThat(msg).contains("50.00");
        assertThat(msg).contains("ALARM");
    }
}

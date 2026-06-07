package com.monitoring.central;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.central.alarm.AlarmService;
import com.monitoring.central.model.SensorReading;
import com.monitoring.central.model.SensorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the REST layer end-to-end using MockMvc (no real HTTP socket needed).
 *
 * Verifies:
 * - Correct HTTP status codes (202 Accepted for measurements, 200 OK for status)
 * - JSON response structure
 * - Batch endpoint processes all readings
 */
@SpringBootTest
@AutoConfigureMockMvc
class MeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlarmService alarmService;

    @Test
    @DisplayName("POST /api/measurements returns 202 Accepted")
    void postMeasurement_returns202() throws Exception {
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 30.0, "WH-01");

        mockMvc.perform(post("/api/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reading)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.sensorId").value("t1"));
    }

    @Test
    @DisplayName("POST /api/measurements/batch returns 202 with count")
    void postBatch_returns202WithCount() throws Exception {
        List<SensorReading> readings = List.of(
                new SensorReading("t1", SensorType.TEMPERATURE, 30.0, "WH-01"),
                new SensorReading("h1", SensorType.HUMIDITY, 40.0, "WH-01")
        );

        mockMvc.perform(post("/api/measurements/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(readings)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("GET /api/status returns UP and alarm count")
    void getStatus_returnsUp() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Central Monitoring Service"))
                .andExpect(jsonPath("$.alarmsRaised").isNumber());
    }
}

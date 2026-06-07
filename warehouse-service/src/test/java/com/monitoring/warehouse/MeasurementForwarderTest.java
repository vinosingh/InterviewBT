package com.monitoring.warehouse;

import com.monitoring.warehouse.config.WarehouseProperties;
import com.monitoring.warehouse.model.SensorReading;
import com.monitoring.warehouse.model.SensorType;
import com.monitoring.warehouse.service.MeasurementForwarder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeasurementForwarder.
 *
 * @Async is NOT active in plain unit tests — we test the forwarding logic
 * synchronously here. The async behaviour is covered by the integration test.
 *
 * Using Mockito to mock RestTemplate so no HTTP calls are made.
 */
@ExtendWith(MockitoExtension.class)
class MeasurementForwarderTest {

    @Mock
    private RestTemplate restTemplate;

    private MeasurementForwarder forwarder;
    private WarehouseProperties props;

    @BeforeEach
    void setUp() {
        props = new WarehouseProperties();
        props.getCentralService().setUrl("http://central-service/api/measurements");
        // Note: call directly (not via @Async proxy) for unit testing
        forwarder = new MeasurementForwarder(restTemplate, props);
    }

    @Test
    @DisplayName("Successful forward calls RestTemplate with correct URL and body")
    void forward_success_callsRestTemplate() {
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 30.0, "WH-01");

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.accepted().body("accepted"));

        forwarder.forward(reading);

        verify(restTemplate).postForEntity(
                "http://central-service/api/measurements",
                reading,
                String.class
        );
    }

    @Test
    @DisplayName("RestClientException is caught and does not propagate")
    void forward_restClientException_doesNotThrow() {
        SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 30.0, "WH-01");

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // Should NOT throw — error is logged internally
        forwarder.forward(reading);

        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
}

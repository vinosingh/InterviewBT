package com.monitoring.warehouse;

import com.monitoring.warehouse.udp.UdpMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for UdpMessageParser — no Spring context needed.
 * These are fast and cover all edge cases of the wire format.
 */
class UdpMessageParserTest {

    private UdpMessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new UdpMessageParser();
    }

    @Test
    @DisplayName("Parses valid temperature message correctly")
    void parse_validTemperatureMessage() {
        Optional<UdpMessageParser.ParsedMessage> result = parser.parse("sensor_id=t1; value=30");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("t1");
        assertThat(result.get().value()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Parses valid humidity message correctly")
    void parse_validHumidityMessage() {
        Optional<UdpMessageParser.ParsedMessage> result = parser.parse("sensor_id=h1; value=40");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("h1");
        assertThat(result.get().value()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Parses message with decimal value")
    void parse_decimalValue() {
        Optional<UdpMessageParser.ParsedMessage> result = parser.parse("sensor_id=t1; value=36.7");

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(36.7);
    }

    @Test
    @DisplayName("Returns empty for null input")
    void parse_nullInput_returnsEmpty() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for blank input")
    void parse_blankInput_returnsEmpty() {
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when sensor_id is missing")
    void parse_missingSensorId_returnsEmpty() {
        assertThat(parser.parse("value=30")).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when value is missing")
    void parse_missingValue_returnsEmpty() {
        assertThat(parser.parse("sensor_id=t1")).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when value is not numeric")
    void parse_nonNumericValue_returnsEmpty() {
        assertThat(parser.parse("sensor_id=t1; value=HOT")).isEmpty();
    }

    @Test
    @DisplayName("Tolerates extra whitespace around fields")
    void parse_extraWhitespace_handled() {
        Optional<UdpMessageParser.ParsedMessage> result =
                parser.parse("  sensor_id = t1 ;  value = 30  ");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("t1");
        assertThat(result.get().value()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Parses message with additional unknown fields (forward compatibility)")
    void parse_extraFields_ignoredGracefully() {
        Optional<UdpMessageParser.ParsedMessage> result =
                parser.parse("sensor_id=t1; value=30; location=north-wing");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("t1");
        assertThat(result.get().value()).isEqualTo(30.0);
    }
}

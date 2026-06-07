package com.monitoring.warehouse.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parses the UDP message format defined in the spec:
 *
 *   sensor_id=t1; value=30
 *   sensor_id=h1; value=40
 *
 * Design:
 * - Returns Optional.empty() on malformed input instead of throwing.
 *   The caller (UdpListener) logs the bad packet and continues — a single
 *   corrupt UDP datagram must not crash the listener thread.
 * - Separated from UdpListener so it can be unit-tested without network I/O.
 */
@Component
public class UdpMessageParser {

    private static final Logger LOG = LoggerFactory.getLogger(UdpMessageParser.class);

    /**
     * Parses a raw UDP payload string into a ParsedMessage.
     *
     * Expected format: "sensor_id=t1; value=30"
     * Fields are semicolon-separated, each field is key=value.
     * Whitespace around separators is tolerated.
     *
     * @param raw the raw string from the UDP datagram
     * @return Optional containing the parsed message, or empty on parse failure
     */
    public Optional<ParsedMessage> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            LOG.warn("Received empty UDP message, ignoring.");
            return Optional.empty();
        }

        try {
            Map<String, String> fields = new HashMap<>();
            String[] parts = raw.trim().split(";");
            for (String part : parts) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2) {
                    fields.put(kv[0].trim(), kv[1].trim());
                }
            }

            String sensorId = fields.get("sensor_id");
            String valueStr = fields.get("value");

            if (sensorId == null || sensorId.isBlank()) {
                LOG.warn("Missing sensor_id in message: '{}'", raw);
                return Optional.empty();
            }
            if (valueStr == null || valueStr.isBlank()) {
                LOG.warn("Missing value in message: '{}'", raw);
                return Optional.empty();
            }

            double value = Double.parseDouble(valueStr);
            return Optional.of(new ParsedMessage(sensorId, value));

        } catch (NumberFormatException e) {
            LOG.warn("Non-numeric value in UDP message '{}': {}", raw, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            LOG.error("Unexpected error parsing UDP message '{}': {}", raw, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Simple value holder for a successfully parsed UDP message.
     * A record keeps this concise and immutable.
     */
    public record ParsedMessage(String sensorId, double value) {}
}

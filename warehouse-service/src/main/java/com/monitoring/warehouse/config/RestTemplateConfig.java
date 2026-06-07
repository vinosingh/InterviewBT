package com.monitoring.warehouse.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configures the RestTemplate used to POST readings to the central service.
 *
 * Timeouts are explicit:
 * - connectTimeout: fail fast if the central service is down.
 * - readTimeout: don't block the forwarder thread indefinitely.
 *
 * In a production system you'd wrap this with Resilience4j CircuitBreaker
 * so sensor readings are buffered locally if the central service is unreachable.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}

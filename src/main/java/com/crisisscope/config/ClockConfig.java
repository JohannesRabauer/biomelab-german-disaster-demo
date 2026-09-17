package com.crisisscope.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes a single {@link Clock} bean so providers/services can derive "now"
 * without calling {@code Instant.now()} directly, keeping them testable.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

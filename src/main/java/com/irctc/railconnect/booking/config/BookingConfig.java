package com.irctc.railconnect.booking.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class BookingConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

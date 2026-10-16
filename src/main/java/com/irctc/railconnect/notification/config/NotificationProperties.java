package com.irctc.railconnect.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        String bookingEventsTopic,
        String consumerGroupId
) {}

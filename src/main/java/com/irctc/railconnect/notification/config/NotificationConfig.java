package com.irctc.railconnect.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    @Bean
    public NewTopic bookingEventsTopic(NotificationProperties props) {
        return TopicBuilder.name(props.bookingEventsTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}

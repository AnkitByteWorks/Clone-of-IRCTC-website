package com.irctc.railconnect.notification.service;

import com.irctc.railconnect.notification.config.NotificationProperties;
import com.irctc.railconnect.notification.event.BookingNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingEventProducer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationProperties notificationProperties;

    public BookingEventProducer(KafkaTemplate<String, Object> kafkaTemplate, NotificationProperties notificationProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationProperties = notificationProperties;
    }

    public void publishEvent(BookingNotificationEvent event) {
        String topic = notificationProperties.bookingEventsTopic();
        String key = event.pnrNumber();
        log.info("Publishing booking notification event {} for PNR {} to topic {}", event.eventType(), key, topic);
        kafkaTemplate.send(topic, key, event);
    }
}

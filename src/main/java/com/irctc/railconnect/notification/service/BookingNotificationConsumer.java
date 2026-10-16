package com.irctc.railconnect.notification.service;

import com.irctc.railconnect.notification.document.NotificationLogDocument;
import com.irctc.railconnect.notification.event.BookingNotificationEvent;
import com.irctc.railconnect.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BookingNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationConsumer.class);

    private final NotificationLogRepository notificationLogRepository;

    public BookingNotificationConsumer(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(
            topics = "${app.notification.booking-events-topic}",
            groupId = "${app.notification.consumer-group-id}"
    )
    public void consumeBookingEvent(BookingNotificationEvent event) {
        log.info("Received booking notification event: eventId={}, eventType={}, pnr={}",
                event.eventId(), event.eventType(), event.pnrNumber());

        if (notificationLogRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate notification event detected, skipping eventId: {}", event.eventId());
            return;
        }

        NotificationLogDocument document = NotificationLogDocument.builder()
                .eventId(event.eventId())
                .eventType(event.eventType())
                .recipientUserId(event.recipientUserId())
                .recipientEmail(event.recipientEmail())
                .pnrNumber(event.pnrNumber())
                .trainNumber(event.trainNumber())
                .trainName(event.trainName())
                .journeyDate(event.journeyDate())
                .subject(event.subject())
                .message(event.message())
                .readStatus(false)
                .occurredAt(event.occurredAt() != null ? event.occurredAt() : Instant.now())
                .processedAt(Instant.now())
                .build();

        notificationLogRepository.save(document);

        log.info("[NOTIFICATION -> {}] {}: {}", event.recipientEmail(), event.subject(), event.message());
    }
}

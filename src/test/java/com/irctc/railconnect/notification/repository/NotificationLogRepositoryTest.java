package com.irctc.railconnect.notification.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.notification.document.NotificationLogDocument;
import com.irctc.railconnect.notification.domain.NotificationEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificationLogRepositoryTest {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        notificationLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save, find by user ID, PNR, check existence by event ID, and query by ID and recipient")
    void shouldSaveAndQueryNotificationLogs() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(60);

        NotificationLogDocument log1 = NotificationLogDocument.builder()
                .eventId("evt-001")
                .eventType(NotificationEventType.BOOKING_CONFIRMED)
                .recipientUserId(101L)
                .recipientEmail("user101@example.com")
                .pnrNumber("1122334455")
                .trainNumber("12309")
                .trainName("Patna Tejas Rajdhani")
                .journeyDate(LocalDate.of(2026, 11, 23))
                .subject("Booking Confirmed")
                .message("Your booking is confirmed")
                .readStatus(false)
                .occurredAt(past)
                .processedAt(past)
                .build();

        NotificationLogDocument log2 = NotificationLogDocument.builder()
                .eventId("evt-002")
                .eventType(NotificationEventType.PASSENGER_PROMOTED)
                .recipientUserId(101L)
                .recipientEmail("user101@example.com")
                .pnrNumber("1122334455")
                .trainNumber("12309")
                .trainName("Patna Tejas Rajdhani")
                .journeyDate(LocalDate.of(2026, 11, 23))
                .subject("Ticket Status Upgraded")
                .message("Upgraded to CNF")
                .readStatus(false)
                .occurredAt(now)
                .processedAt(now)
                .build();

        notificationLogRepository.save(log1);
        notificationLogRepository.save(log2);

        // 1. Exists by eventId
        assertThat(notificationLogRepository.existsByEventId("evt-001")).isTrue();
        assertThat(notificationLogRepository.existsByEventId("evt-999")).isFalse();

        // 2. Find by recipient user ordered by occurredAt desc
        List<NotificationLogDocument> userLogs = notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(101L);
        assertThat(userLogs).hasSize(2);
        assertThat(userLogs.get(0).getEventId()).isEqualTo("evt-002");
        assertThat(userLogs.get(1).getEventId()).isEqualTo("evt-001");

        // 3. Find by PNR ordered by occurredAt desc
        List<NotificationLogDocument> pnrLogs = notificationLogRepository.findByPnrNumberOrderByOccurredAtDesc("1122334455");
        assertThat(pnrLogs).hasSize(2);

        // 4. Find by ID and recipient user
        Optional<NotificationLogDocument> found = notificationLogRepository.findByIdAndRecipientUserId(log1.getId(), 101L);
        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo("evt-001");

        Optional<NotificationLogDocument> notFound = notificationLogRepository.findByIdAndRecipientUserId(log1.getId(), 999L);
        assertThat(notFound).isEmpty();
    }
}

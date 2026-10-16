package com.irctc.railconnect.notification.document;

import com.irctc.railconnect.notification.domain.NotificationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_logs")
public class NotificationLogDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private NotificationEventType eventType;

    @Indexed
    private Long recipientUserId;

    private String recipientEmail;

    @Indexed
    private String pnrNumber;

    private String trainNumber;

    private String trainName;

    private LocalDate journeyDate;

    private String subject;

    private String message;

    private boolean readStatus;

    private Instant occurredAt;

    private Instant processedAt;
}

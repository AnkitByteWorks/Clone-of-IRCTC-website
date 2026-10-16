package com.irctc.railconnect.notification.event;

import com.irctc.railconnect.notification.domain.NotificationEventType;

import java.time.Instant;
import java.time.LocalDate;

public record BookingNotificationEvent(
        String eventId,
        NotificationEventType eventType,
        Long recipientUserId,
        String recipientEmail,
        String pnrNumber,
        String trainNumber,
        String trainName,
        LocalDate journeyDate,
        String subject,
        String message,
        Instant occurredAt
) {}

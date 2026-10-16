package com.irctc.railconnect.notification.dto;

import com.irctc.railconnect.notification.domain.NotificationEventType;

import java.time.Instant;
import java.time.LocalDate;

public record NotificationResponse(
        String id,
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
        boolean readStatus,
        Instant occurredAt,
        Instant processedAt
) {}

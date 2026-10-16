package com.irctc.railconnect.inventory.dto;

import com.irctc.railconnect.inventory.domain.BerthType;

import java.time.Instant;
import java.time.LocalDate;

public record SeatLockResponse(
        String trainNumber,
        LocalDate journeyDate,
        String coachNumber,
        int seatNumber,
        BerthType berthType,
        String lockedByEmail,
        long ttlSeconds,
        Instant expiresAt
) {
}

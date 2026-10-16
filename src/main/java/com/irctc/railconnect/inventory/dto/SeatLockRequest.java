package com.irctc.railconnect.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record SeatLockRequest(
        @NotBlank(message = "Train number is required")
        String trainNumber,

        @NotNull(message = "Journey date is required")
        LocalDate journeyDate,

        @NotBlank(message = "Class code is required")
        String classCode,

        @NotBlank(message = "Coach number is required")
        String coachNumber,

        @Positive(message = "Seat number must be positive")
        int seatNumber
) {
}

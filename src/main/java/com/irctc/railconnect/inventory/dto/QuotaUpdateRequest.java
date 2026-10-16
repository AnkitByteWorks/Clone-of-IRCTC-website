package com.irctc.railconnect.inventory.dto;

import com.irctc.railconnect.inventory.domain.QuotaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record QuotaUpdateRequest(
        @NotBlank(message = "Train number is required")
        String trainNumber,

        @NotNull(message = "Journey date is required")
        LocalDate journeyDate,

        @NotBlank(message = "Class code is required")
        String classCode,

        @NotNull(message = "Quota type is required")
        QuotaType quotaType,

        @PositiveOrZero(message = "Total confirmed seats must be >= 0")
        int totalConfirmedSeats,

        @PositiveOrZero(message = "Total RAC slots must be >= 0")
        int totalRacSlots,

        @PositiveOrZero(message = "Max waitlist limit must be >= 0")
        int maxWaitlistLimit
) {
}

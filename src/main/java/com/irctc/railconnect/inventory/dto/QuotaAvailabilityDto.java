package com.irctc.railconnect.inventory.dto;

import com.irctc.railconnect.inventory.domain.AvailabilityStatus;
import com.irctc.railconnect.inventory.domain.QuotaType;

import java.math.BigDecimal;

public record QuotaAvailabilityDto(
        String classCode,
        QuotaType quotaType,
        AvailabilityStatus availabilityStatus,
        String displayStatus,
        int availableConfirmedSeats,
        int availableRacSlots,
        int currentWaitlistCount,
        int maxWaitlistLimit,
        BigDecimal baseFare
) {
}

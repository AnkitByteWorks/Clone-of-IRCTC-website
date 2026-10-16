package com.irctc.railconnect.inventory.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainAvailabilityResponse(
        String trainNumber,
        String trainName,
        LocalDate journeyDate,
        List<QuotaAvailabilityDto> quotas
) {
}

package com.irctc.railconnect.catalog.dto;

import java.math.BigDecimal;

public record ClassFareDto(
        String classCode,
        int totalSeats,
        BigDecimal generalFare,
        BigDecimal tatkalFare
) {}

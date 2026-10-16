package com.irctc.railconnect.inventory.dto;

import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.SeatStatus;

public record SeatDto(
        Long seatId,
        String coachNumber,
        int seatNumber,
        BerthType berthType,
        SeatStatus effectiveStatus,
        boolean lockedByCurrentUser
) {
}

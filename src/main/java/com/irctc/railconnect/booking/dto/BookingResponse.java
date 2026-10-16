package com.irctc.railconnect.booking.dto;

import com.irctc.railconnect.booking.domain.BookingStatus;
import com.irctc.railconnect.booking.domain.PaymentStatus;
import com.irctc.railconnect.inventory.domain.QuotaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        String pnrNumber,
        String trainNumber,
        String trainName,
        String fromStationCode,
        String toStationCode,
        LocalDate journeyDate,
        String departureTime,
        String arrivalTime,
        String classCode,
        QuotaType quotaType,
        BookingStatus bookingStatus,
        BigDecimal totalFare,
        String transactionReference,
        PaymentStatus paymentStatus,
        BigDecimal refundAmount,
        Instant bookedAt,
        Instant cancelledAt,
        List<PassengerTicketDto> passengers
) {
}

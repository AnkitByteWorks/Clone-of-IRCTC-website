package com.irctc.railconnect.booking.dto;

import com.irctc.railconnect.booking.domain.PaymentMethod;
import com.irctc.railconnect.inventory.domain.QuotaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateBookingRequest(
        @NotBlank(message = "Train number is required")
        String trainNumber,

        @NotBlank(message = "Source station code is required")
        String fromStationCode,

        @NotBlank(message = "Destination station code is required")
        String toStationCode,

        @NotNull(message = "Journey date is required")
        LocalDate journeyDate,

        @NotBlank(message = "Class code is required")
        String classCode,

        @NotNull(message = "Quota type is required")
        QuotaType quotaType,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "At least one passenger is required")
        List<@Valid PassengerBookingInput> passengers
) {
    public CreateBookingRequest(
            String trainNumber,
            String fromStationCode,
            String toStationCode,
            LocalDate journeyDate,
            String classCode,
            QuotaType quotaType,
            List<PassengerBookingInput> passengers,
            PaymentMethod paymentMethod
    ) {
        this(trainNumber, fromStationCode, toStationCode, journeyDate, classCode, quotaType, paymentMethod, passengers);
    }
}

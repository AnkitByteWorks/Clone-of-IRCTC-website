package com.irctc.railconnect.user.dto;

import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavedPassengerRequest(
        @NotBlank(message = "Passenger name is required")
        String passengerName,

        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 125, message = "Age must not exceed 125")
        int age,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotNull(message = "Berth preference is required")
        BerthPreference berthPreference,

        boolean seniorCitizenOptIn
) {
}

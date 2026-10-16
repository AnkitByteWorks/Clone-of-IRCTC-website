package com.irctc.railconnect.booking.dto;

import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;

public record PassengerTicketDto(
        Long passengerId,
        String passengerName,
        int age,
        Gender gender,
        BerthPreference berthPreference,
        PassengerTicketStatus bookingStatus,
        String bookingStatusDetails,
        PassengerTicketStatus currentStatus,
        String currentStatusDetails,
        String currentCoach,
        Integer currentSeatNumber,
        BerthType currentBerthType
) {
}

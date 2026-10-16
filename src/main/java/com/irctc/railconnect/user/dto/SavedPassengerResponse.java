package com.irctc.railconnect.user.dto;

import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.entity.SavedPassengerEntity;

public record SavedPassengerResponse(
        Long id,
        String passengerName,
        int age,
        Gender gender,
        BerthPreference berthPreference,
        boolean seniorCitizenOptIn
) {
    public static SavedPassengerResponse fromEntity(SavedPassengerEntity entity) {
        return new SavedPassengerResponse(
                entity.getId(),
                entity.getPassengerName(),
                entity.getAge(),
                entity.getGender(),
                entity.getBerthPreference(),
                entity.isSeniorCitizenOptIn()
        );
    }
}

package com.irctc.railconnect.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StationRequest(
        @NotBlank(message = "Station code cannot be blank")
        String stationCode,

        @NotBlank(message = "Station name cannot be blank")
        String stationName,

        @NotBlank(message = "City cannot be blank")
        String city,

        @NotBlank(message = "State cannot be blank")
        String state,

        @NotBlank(message = "Zone cannot be blank")
        String zone,

        @Positive(message = "Number of platforms must be positive")
        int numberOfPlatforms
) {}

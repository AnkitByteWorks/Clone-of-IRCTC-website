package com.irctc.railconnect.catalog.dto;

public record StationResponse(
        String id,
        String stationCode,
        String stationName,
        String city,
        String state,
        String zone,
        int numberOfPlatforms
) {}

package com.irctc.railconnect.catalog.dto;

import com.irctc.railconnect.catalog.document.TrainType;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public record TrainSearchResultDto(
        String trainNumber,
        String trainName,
        TrainType trainType,
        String fromStationCode,
        String fromStationName,
        String departureTime,
        String toStationCode,
        String toStationName,
        String arrivalTime,
        int durationMinutes,
        int distanceKm,
        Set<DayOfWeek> runningDays,
        List<ClassFareDto> availableClasses
) {}

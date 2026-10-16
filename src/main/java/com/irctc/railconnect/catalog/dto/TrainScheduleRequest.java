package com.irctc.railconnect.catalog.dto;

import com.irctc.railconnect.catalog.document.CoachClassConfig;
import com.irctc.railconnect.catalog.document.RouteStop;
import com.irctc.railconnect.catalog.document.TrainType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public record TrainScheduleRequest(
        @NotBlank(message = "Train number cannot be blank")
        String trainNumber,

        @NotBlank(message = "Train name cannot be blank")
        String trainName,

        @NotNull(message = "Train type cannot be null")
        TrainType trainType,

        @NotBlank(message = "Source station code cannot be blank")
        String sourceStationCode,

        @NotBlank(message = "Destination station code cannot be blank")
        String destinationStationCode,

        @NotEmpty(message = "Running days cannot be empty")
        Set<DayOfWeek> runningDays,

        @NotEmpty(message = "Route stops cannot be empty")
        List<@Valid RouteStop> routeStops,

        @NotEmpty(message = "Coach classes cannot be empty")
        List<@Valid CoachClassConfig> coachClasses
) {}

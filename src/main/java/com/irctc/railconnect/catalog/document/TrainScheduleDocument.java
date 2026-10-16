package com.irctc.railconnect.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "train_schedules")
public class TrainScheduleDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String trainNumber;

    @Indexed
    private String trainName;

    private TrainType trainType;

    @Indexed
    private String sourceStationCode;

    @Indexed
    private String destinationStationCode;

    private Set<DayOfWeek> runningDays;

    private List<RouteStop> routeStops;

    private List<CoachClassConfig> coachClasses;

    private boolean active;
}

package com.irctc.railconnect.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    private int stopSequence;

    @Indexed
    private String stationCode;

    private String stationName;

    private String arrivalTime;

    private String departureTime;

    private int haltMinutes;

    private int dayOffset;

    private int distanceFromSourceKm;

    private String platformNumber;

    // Record-style accessor aliases
    public int stopSequence() { return stopSequence; }
    public String stationCode() { return stationCode; }
    public String stationName() { return stationName; }
    public String arrivalTime() { return arrivalTime; }
    public String departureTime() { return departureTime; }
    public int haltMinutes() { return haltMinutes; }
    public int dayOffset() { return dayOffset; }
    public int distanceFromSourceKm() { return distanceFromSourceKm; }
    public String platformNumber() { return platformNumber; }
}

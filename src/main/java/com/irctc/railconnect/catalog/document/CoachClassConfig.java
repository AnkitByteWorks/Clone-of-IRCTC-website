package com.irctc.railconnect.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachClassConfig {

    private String classCode;

    private int totalCoaches;

    private int seatsPerCoach;

    private BigDecimal baseFarePerKm;

    private BigDecimal tatkalSurcharge;

    // Record-style accessor aliases
    public String classCode() { return classCode; }
    public int totalCoaches() { return totalCoaches; }
    public int seatsPerCoach() { return seatsPerCoach; }
    public BigDecimal baseFarePerKm() { return baseFarePerKm; }
    public BigDecimal tatkalSurcharge() { return tatkalSurcharge; }
}

package com.irctc.railconnect.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "app.inventory")
public record InventoryProperties(
        @DefaultValue("300") int seatLockTtlSeconds,
        @DefaultValue("0.80") double generalQuotaRatio,
        @DefaultValue("0.20") double tatkalQuotaRatio,
        @DefaultValue("4") int defaultGeneralRacSlots,
        @DefaultValue("15") int defaultGeneralWaitlistLimit,
        @DefaultValue("10") int defaultTatkalWaitlistLimit,
        Map<String, String> coachPrefixByClass,
        Set<String> chairCarClasses
) {
    public InventoryProperties {
        if (coachPrefixByClass == null || coachPrefixByClass.isEmpty()) {
            coachPrefixByClass = Map.of(
                    "1A", "H",
                    "2A", "A",
                    "3A", "B",
                    "SL", "S",
                    "CC", "C",
                    "EC", "E"
            );
        }
        if (chairCarClasses == null || chairCarClasses.isEmpty()) {
            chairCarClasses = Set.of("CC", "EC");
        }
    }
}

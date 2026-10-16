package com.irctc.railconnect.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Set;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        @DefaultValue("6") int maxGeneralPassengersPerPnr,
        @DefaultValue("4") int maxTatkalPassengersPerPnr,
        @DefaultValue("10") int pnrLength,
        @DefaultValue("0.75") double cancellationRefundRatio,
        @DefaultValue("false") boolean enforceTatkalTimeWindow,
        @DefaultValue("1") int tatkalAdvanceDays,
        @DefaultValue("10:00") String tatkalAcOpenTime,
        @DefaultValue("11:00") String tatkalNonAcOpenTime,
        Set<String> nonAcClasses
) {
    public BookingProperties {
        if (nonAcClasses == null || nonAcClasses.isEmpty()) {
            nonAcClasses = Set.of("SL", "2S");
        }
    }
}

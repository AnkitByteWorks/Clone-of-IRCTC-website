package com.irctc.railconnect.booking.domain;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PARTIALLY_CONFIRMED,
    RAC,
    WAITLISTED,
    CANCELLED,
    PAYMENT_FAILED
}

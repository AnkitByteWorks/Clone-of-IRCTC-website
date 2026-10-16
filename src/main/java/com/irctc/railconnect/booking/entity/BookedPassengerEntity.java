package com.irctc.railconnect.booking.entity;

import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booked_passengers",
        indexes = {
                @Index(name = "idx_bp_booking", columnList = "booking_id"),
                @Index(name = "idx_bp_promotion", columnList = "current_status, current_sequence_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookedPassengerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "berth_preference", nullable = false, length = 30)
    private BerthPreference berthPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 10)
    private PassengerTicketStatus bookingStatus;

    @Column(name = "booking_status_details", nullable = false, length = 50)
    private String bookingStatusDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 10)
    private PassengerTicketStatus currentStatus;

    @Column(name = "current_coach", length = 10)
    private String currentCoach;

    @Column(name = "current_seat_number")
    private Integer currentSeatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_berth_type", length = 20)
    private BerthType currentBerthType;

    @Column(name = "current_sequence_number")
    private Integer currentSequenceNumber;

    public String getCurrentStatusDetails() {
        if (currentStatus == PassengerTicketStatus.CNF) {
            return "CNF/" + currentCoach + "/" + currentSeatNumber + "/" + currentBerthType;
        } else if (currentStatus == PassengerTicketStatus.RAC) {
            return "RAC/" + currentSequenceNumber;
        } else if (currentStatus == PassengerTicketStatus.WL) {
            return "WL/" + currentSequenceNumber;
        } else if (currentStatus == PassengerTicketStatus.CAN) {
            return "CAN";
        }
        return currentStatus.name();
    }
}

package com.irctc.railconnect.booking.entity;

import com.irctc.railconnect.booking.domain.BookingStatus;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookings_pnr", columnNames = "pnr_number")
        },
        indexes = {
                @Index(name = "idx_booking_user", columnList = "user_id"),
                @Index(name = "idx_booking_train_date", columnList = "train_number, journey_date, class_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pnr_number", nullable = false, unique = true, length = 10)
    private String pnrNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "train_number", nullable = false, length = 20)
    private String trainNumber;

    @Column(name = "train_name", nullable = false, length = 150)
    private String trainName;

    @Column(name = "from_station_code", nullable = false, length = 10)
    private String fromStationCode;

    @Column(name = "to_station_code", nullable = false, length = 10)
    private String toStationCode;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Column(name = "departure_time", nullable = false, length = 10)
    private String departureTime;

    @Column(name = "arrival_time", nullable = false, length = 10)
    private String arrivalTime;

    @Column(name = "class_code", nullable = false, length = 10)
    private String classCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 30)
    private QuotaType quotaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    private BookingStatus bookingStatus;

    @Column(name = "total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookedPassengerEntity> passengers = new ArrayList<>();

    public void addPassenger(BookedPassengerEntity passenger) {
        if (this.passengers == null) {
            this.passengers = new ArrayList<>();
        }
        this.passengers.add(passenger);
        passenger.setBooking(this);
    }

    @PrePersist
    void prePersist() {
        if (this.bookedAt == null) {
            this.bookedAt = Instant.now();
        }
    }
}

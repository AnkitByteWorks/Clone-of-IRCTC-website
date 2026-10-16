package com.irctc.railconnect.inventory.entity;

import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "coach_seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coach_seat_unique",
                        columnNames = {"train_number", "journey_date", "coach_number", "seat_number"}
                )
        },
        indexes = {
                @Index(name = "idx_seat_lookup", columnList = "train_number, journey_date, class_code, status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_number", nullable = false, length = 20)
    private String trainNumber;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Column(name = "class_code", nullable = false, length = 10)
    private String classCode;

    @Column(name = "coach_number", nullable = false, length = 10)
    private String coachNumber;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "berth_type", nullable = false, length = 20)
    private BerthType berthType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @Column(name = "locked_by_user_email", length = 150)
    private String lockedByUserEmail;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Version
    private Long version;
}

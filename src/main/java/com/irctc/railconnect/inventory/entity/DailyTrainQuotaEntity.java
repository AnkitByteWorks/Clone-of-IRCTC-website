package com.irctc.railconnect.inventory.entity;

import com.irctc.railconnect.inventory.domain.AvailabilityStatus;
import com.irctc.railconnect.inventory.domain.QuotaType;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_train_quotas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_quota_train_date_class_quota",
                        columnNames = {"train_number", "journey_date", "class_code", "quota_type"}
                )
        },
        indexes = {
                @Index(name = "idx_quota_train_date", columnList = "train_number, journey_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrainQuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_number", nullable = false, length = 20)
    private String trainNumber;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Column(name = "class_code", nullable = false, length = 10)
    private String classCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 30)
    private QuotaType quotaType;

    @Column(name = "total_confirmed_seats", nullable = false)
    private int totalConfirmedSeats;

    @Column(name = "available_confirmed_seats", nullable = false)
    private int availableConfirmedSeats;

    @Column(name = "total_rac_slots", nullable = false)
    private int totalRacSlots;

    @Column(name = "available_rac_slots", nullable = false)
    private int availableRacSlots;

    @Column(name = "max_waitlist_limit", nullable = false)
    private int maxWaitlistLimit;

    @Column(name = "current_waitlist_count", nullable = false)
    private int currentWaitlistCount;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Version
    private Long version;

    public AvailabilityStatus getCurrentAvailabilityStatus() {
        if (availableConfirmedSeats > 0) {
            return AvailabilityStatus.AVAILABLE;
        }
        if (availableRacSlots > 0) {
            return AvailabilityStatus.RAC;
        }
        if (currentWaitlistCount < maxWaitlistLimit) {
            return AvailabilityStatus.WAITLIST;
        }
        return AvailabilityStatus.REGRET;
    }

    public String getDisplayStatusText() {
        if (availableConfirmedSeats > 0) {
            return "AVAILABLE-" + availableConfirmedSeats;
        }
        if (availableRacSlots > 0) {
            return "RAC-" + availableRacSlots;
        }
        if (currentWaitlistCount < maxWaitlistLimit) {
            return "WL-" + (currentWaitlistCount == 0 ? 1 : currentWaitlistCount);
        }
        return "REGRET";
    }
}

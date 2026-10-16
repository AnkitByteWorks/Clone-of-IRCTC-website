package com.irctc.railconnect.inventory.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.inventory.domain.AvailabilityStatus;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import com.irctc.railconnect.inventory.entity.CoachSeatEntity;
import com.irctc.railconnect.inventory.entity.DailyTrainQuotaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class InventoryRepositoryIntegrationTest {

    @Autowired
    private DailyTrainQuotaRepository dailyTrainQuotaRepository;

    @Autowired
    private CoachSeatRepository coachSeatRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should transition availability status from AVAILABLE to RAC to WAITLIST to REGRET")
    void shouldTransitionAvailabilityStatusFromAvailableToRacToWaitlistToRegret() {
        LocalDate journeyDate = LocalDate.of(2026, 11, 15);

        DailyTrainQuotaEntity quota = DailyTrainQuotaEntity.builder()
                .trainNumber("12309")
                .journeyDate(journeyDate)
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .totalConfirmedSeats(2)
                .availableConfirmedSeats(2)
                .totalRacSlots(2)
                .availableRacSlots(2)
                .maxWaitlistLimit(5)
                .currentWaitlistCount(0)
                .baseFare(new BigDecimal("1550.00"))
                .build();

        DailyTrainQuotaEntity saved = dailyTrainQuotaRepository.saveAndFlush(quota);

        // 1. Initially AVAILABLE
        assertThat(saved.getCurrentAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(saved.getDisplayStatusText()).isEqualTo("AVAILABLE-2");

        // 2. Decrement available confirmed seats to 0 -> RAC
        saved.setAvailableConfirmedSeats(0);
        saved = dailyTrainQuotaRepository.saveAndFlush(saved);
        assertThat(saved.getCurrentAvailabilityStatus()).isEqualTo(AvailabilityStatus.RAC);
        assertThat(saved.getDisplayStatusText()).isEqualTo("RAC-2");

        // 3. Decrement available RAC slots to 0 -> WAITLIST (WL-1)
        saved.setAvailableRacSlots(0);
        saved = dailyTrainQuotaRepository.saveAndFlush(saved);
        assertThat(saved.getCurrentAvailabilityStatus()).isEqualTo(AvailabilityStatus.WAITLIST);
        assertThat(saved.getDisplayStatusText()).isEqualTo("WL-1");

        // 4. Increment waitlist count to maxWaitlistLimit -> REGRET
        saved.setCurrentWaitlistCount(saved.getMaxWaitlistLimit());
        saved = dailyTrainQuotaRepository.saveAndFlush(saved);
        assertThat(saved.getCurrentAvailabilityStatus()).isEqualTo(AvailabilityStatus.REGRET);
        assertThat(saved.getDisplayStatusText()).isEqualTo("REGRET");
    }

    @Test
    @DisplayName("Should enforce unique daily quota constraint and support pessimistic write lock")
    void shouldEnforceUniqueDailyQuotaConstraintAndSupportPessimisticWriteLock() {
        LocalDate journeyDate = LocalDate.of(2026, 11, 20);

        DailyTrainQuotaEntity initialQuota = DailyTrainQuotaEntity.builder()
                .trainNumber("12309")
                .journeyDate(journeyDate)
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .totalConfirmedSeats(50)
                .availableConfirmedSeats(50)
                .totalRacSlots(10)
                .availableRacSlots(10)
                .maxWaitlistLimit(20)
                .currentWaitlistCount(0)
                .baseFare(new BigDecimal("1550.00"))
                .build();

        DailyTrainQuotaEntity saved = dailyTrainQuotaRepository.saveAndFlush(initialQuota);
        entityManager.clear();

        // Verify pessimistic write lock query fetches the entity successfully
        Optional<DailyTrainQuotaEntity> lockedQuota = dailyTrainQuotaRepository.findAndLockQuota(
                "12309",
                journeyDate,
                "3A",
                QuotaType.GENERAL
        );
        assertThat(lockedQuota).isPresent();
        assertThat(lockedQuota.get().getId()).isEqualTo(saved.getId());

        // Attempt to insert duplicate (trainNumber, journeyDate, classCode, quotaType)
        DailyTrainQuotaEntity duplicateQuota = DailyTrainQuotaEntity.builder()
                .trainNumber("12309")
                .journeyDate(journeyDate)
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .totalConfirmedSeats(30)
                .availableConfirmedSeats(30)
                .totalRacSlots(6)
                .availableRacSlots(6)
                .maxWaitlistLimit(15)
                .currentWaitlistCount(0)
                .baseFare(new BigDecimal("1550.00"))
                .build();

        assertThatThrownBy(() -> dailyTrainQuotaRepository.saveAndFlush(duplicateQuota))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find available seat matching preferred berth type or fallback to any seat")
    void shouldFindAvailableSeatMatchingPreferredBerthTypeOrFallbackToAnySeat() {
        LocalDate journeyDate = LocalDate.of(2026, 11, 25);
        String trainNumber = "12309";
        String classCode = "3A";
        String coachNumber = "B1";

        // Seed Coach B1 seats
        CoachSeatEntity seat1 = CoachSeatEntity.builder()
                .trainNumber(trainNumber)
                .journeyDate(journeyDate)
                .classCode(classCode)
                .coachNumber(coachNumber)
                .seatNumber(1)
                .berthType(BerthType.LOWER)
                .status(SeatStatus.AVAILABLE)
                .build();

        CoachSeatEntity seat2 = CoachSeatEntity.builder()
                .trainNumber(trainNumber)
                .journeyDate(journeyDate)
                .classCode(classCode)
                .coachNumber(coachNumber)
                .seatNumber(2)
                .berthType(BerthType.MIDDLE)
                .status(SeatStatus.AVAILABLE)
                .build();

        CoachSeatEntity seat7 = CoachSeatEntity.builder()
                .trainNumber(trainNumber)
                .journeyDate(journeyDate)
                .classCode(classCode)
                .coachNumber(coachNumber)
                .seatNumber(7)
                .berthType(BerthType.SIDE_LOWER)
                .status(SeatStatus.AVAILABLE)
                .build();

        coachSeatRepository.save(seat1);
        coachSeatRepository.save(seat2);
        coachSeatRepository.save(seat7);
        coachSeatRepository.flush();

        // 1. Query for preferred berth type: SIDE_LOWER with AVAILABLE status -> returns B1-7
        Optional<CoachSeatEntity> sideLowerSeat = coachSeatRepository
                .findFirstByTrainNumberAndJourneyDateAndClassCodeAndBerthTypeAndStatusOrderByCoachNumberAscSeatNumberAsc(
                        trainNumber,
                        journeyDate,
                        classCode,
                        BerthType.SIDE_LOWER,
                        SeatStatus.AVAILABLE
                );

        assertThat(sideLowerSeat).isPresent();
        assertThat(sideLowerSeat.get().getCoachNumber()).isEqualTo("B1");
        assertThat(sideLowerSeat.get().getSeatNumber()).isEqualTo(7);
        assertThat(sideLowerSeat.get().getBerthType()).isEqualTo(BerthType.SIDE_LOWER);

        // 2. Mark B1-7 as BOOKED
        CoachSeatEntity bookedSeat7 = sideLowerSeat.get();
        bookedSeat7.setStatus(SeatStatus.BOOKED);
        coachSeatRepository.saveAndFlush(bookedSeat7);

        // 3. Preferred query for SIDE_LOWER now returns empty
        Optional<CoachSeatEntity> unavailableSideLower = coachSeatRepository
                .findFirstByTrainNumberAndJourneyDateAndClassCodeAndBerthTypeAndStatusOrderByCoachNumberAscSeatNumberAsc(
                        trainNumber,
                        journeyDate,
                        classCode,
                        BerthType.SIDE_LOWER,
                        SeatStatus.AVAILABLE
                );
        assertThat(unavailableSideLower).isEmpty();

        // 4. Fallback to any available seat -> returns next available seat B1-1
        Optional<CoachSeatEntity> fallbackSeat = coachSeatRepository
                .findFirstByTrainNumberAndJourneyDateAndClassCodeAndStatusOrderByCoachNumberAscSeatNumberAsc(
                        trainNumber,
                        journeyDate,
                        classCode,
                        SeatStatus.AVAILABLE
                );

        assertThat(fallbackSeat).isPresent();
        assertThat(fallbackSeat.get().getCoachNumber()).isEqualTo("B1");
        assertThat(fallbackSeat.get().getSeatNumber()).isEqualTo(1);
        assertThat(fallbackSeat.get().getBerthType()).isEqualTo(BerthType.LOWER);
    }
}

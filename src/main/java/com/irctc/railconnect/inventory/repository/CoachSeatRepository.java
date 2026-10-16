package com.irctc.railconnect.inventory.repository;

import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import com.irctc.railconnect.inventory.entity.CoachSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoachSeatRepository extends JpaRepository<CoachSeatEntity, Long> {

    List<CoachSeatEntity> findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
            String trainNumber,
            LocalDate journeyDate,
            String classCode
    );

    List<CoachSeatEntity> findByTrainNumberAndJourneyDateAndClassCodeAndStatusOrderByCoachNumberAscSeatNumberAsc(
            String trainNumber,
            LocalDate journeyDate,
            String classCode,
            SeatStatus status
    );

    Optional<CoachSeatEntity> findFirstByTrainNumberAndJourneyDateAndClassCodeAndBerthTypeAndStatusOrderByCoachNumberAscSeatNumberAsc(
            String trainNumber,
            LocalDate journeyDate,
            String classCode,
            BerthType berthType,
            SeatStatus status
    );

    Optional<CoachSeatEntity> findFirstByTrainNumberAndJourneyDateAndClassCodeAndStatusOrderByCoachNumberAscSeatNumberAsc(
            String trainNumber,
            LocalDate journeyDate,
            String classCode,
            SeatStatus status
    );
}

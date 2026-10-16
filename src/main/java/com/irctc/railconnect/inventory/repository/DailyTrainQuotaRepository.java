package com.irctc.railconnect.inventory.repository;

import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.entity.DailyTrainQuotaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTrainQuotaRepository extends JpaRepository<DailyTrainQuotaEntity, Long> {

    List<DailyTrainQuotaEntity> findByTrainNumberAndJourneyDate(String trainNumber, LocalDate journeyDate);

    Optional<DailyTrainQuotaEntity> findByTrainNumberAndJourneyDateAndClassCodeAndQuotaType(
            String trainNumber,
            LocalDate journeyDate,
            String classCode,
            QuotaType quotaType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DailyTrainQuotaEntity q WHERE q.trainNumber = :trainNumber AND q.journeyDate = :journeyDate AND q.classCode = :classCode AND q.quotaType = :quotaType")
    Optional<DailyTrainQuotaEntity> findAndLockQuota(
            @Param("trainNumber") String trainNumber,
            @Param("journeyDate") LocalDate journeyDate,
            @Param("classCode") String classCode,
            @Param("quotaType") QuotaType quotaType
    );
}

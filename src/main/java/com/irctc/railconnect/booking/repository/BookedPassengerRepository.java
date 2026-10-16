package com.irctc.railconnect.booking.repository;

import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.booking.entity.BookedPassengerEntity;
import com.irctc.railconnect.inventory.domain.QuotaType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookedPassengerRepository extends JpaRepository<BookedPassengerEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM BookedPassengerEntity p JOIN FETCH p.booking b WHERE b.trainNumber = :trainNumber AND b.journeyDate = :journeyDate AND b.classCode = :classCode AND b.quotaType = :quotaType AND p.currentStatus = :status ORDER BY p.currentSequenceNumber ASC, p.id ASC")
    List<BookedPassengerEntity> findPassengersForPromotion(
            @Param("trainNumber") String trainNumber,
            @Param("journeyDate") LocalDate journeyDate,
            @Param("classCode") String classCode,
            @Param("quotaType") QuotaType quotaType,
            @Param("status") PassengerTicketStatus status
    );
}

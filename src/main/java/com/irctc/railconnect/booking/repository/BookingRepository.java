package com.irctc.railconnect.booking.repository;

import com.irctc.railconnect.booking.entity.BookingEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    Optional<BookingEntity> findByPnrNumber(String pnrNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingEntity b WHERE b.pnrNumber = :pnrNumber")
    Optional<BookingEntity> findAndLockByPnrNumber(@Param("pnrNumber") String pnrNumber);

    boolean existsByPnrNumber(String pnrNumber);

    List<BookingEntity> findByUserIdOrderByBookedAtDesc(Long userId);
}

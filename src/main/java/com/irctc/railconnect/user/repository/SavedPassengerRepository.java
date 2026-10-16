package com.irctc.railconnect.user.repository;

import com.irctc.railconnect.user.entity.SavedPassengerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPassengerRepository extends JpaRepository<SavedPassengerEntity, Long> {

    List<SavedPassengerEntity> findByUserIdOrderByIdAsc(Long userId);

    Optional<SavedPassengerEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}

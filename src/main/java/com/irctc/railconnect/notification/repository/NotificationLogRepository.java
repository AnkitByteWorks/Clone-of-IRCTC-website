package com.irctc.railconnect.notification.repository;

import com.irctc.railconnect.notification.document.NotificationLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLogDocument, String> {

    List<NotificationLogDocument> findByRecipientUserIdOrderByOccurredAtDesc(Long recipientUserId);

    List<NotificationLogDocument> findByPnrNumberOrderByOccurredAtDesc(String pnrNumber);

    boolean existsByEventId(String eventId);

    Optional<NotificationLogDocument> findByIdAndRecipientUserId(String id, Long recipientUserId);
}

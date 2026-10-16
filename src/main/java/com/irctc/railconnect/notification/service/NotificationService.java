package com.irctc.railconnect.notification.service;

import com.irctc.railconnect.common.exception.ResourceNotFoundException;
import com.irctc.railconnect.notification.document.NotificationLogDocument;
import com.irctc.railconnect.notification.dto.NotificationResponse;
import com.irctc.railconnect.notification.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long userId, String notificationId) {
        NotificationLogDocument document = notificationLogRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!document.getRecipientUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        document.setReadStatus(true);
        NotificationLogDocument updated = notificationLogRepository.save(document);
        return toResponse(updated);
    }

    private NotificationResponse toResponse(NotificationLogDocument doc) {
        return new NotificationResponse(
                doc.getId(),
                doc.getEventId(),
                doc.getEventType(),
                doc.getRecipientUserId(),
                doc.getRecipientEmail(),
                doc.getPnrNumber(),
                doc.getTrainNumber(),
                doc.getTrainName(),
                doc.getJourneyDate(),
                doc.getSubject(),
                doc.getMessage(),
                doc.isReadStatus(),
                doc.getOccurredAt(),
                doc.getProcessedAt()
        );
    }
}

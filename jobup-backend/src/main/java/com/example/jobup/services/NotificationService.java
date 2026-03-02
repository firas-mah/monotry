package com.example.jobup.services;

import com.example.jobup.dto.NotificationDto;
import com.example.jobup.entities.Notification;
import com.example.jobup.entities.NotificationType;
import com.example.jobup.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDto createNotification(
            String recipientId, String recipientName,
            String senderId, String senderName,
            String refId, String refTitle,
            NotificationType type,
            String customMessage
    ) {
        String message = (customMessage != null) ? customMessage : defaultMessage(type, senderName, refTitle);
        String actionUrl = defaultActionUrl(type, refId);

        Notification n = Notification.builder()
                .recipientId(recipientId)
                .recipientName(recipientName)
                .senderId(senderId)
                .senderName(senderName)
                .refId(refId)
                .refTitle(refTitle)
                .type(type)
                .message(message)
                .actionUrl(actionUrl)
                .build();

        Notification saved = notificationRepository.save(n);
        NotificationDto dto = toDto(saved);

        // WebSocket push (adjust destination to your STOMP config)
        sendRealtime(recipientId, dto);

        return dto;
    }

    public List<NotificationDto> getNotificationsByUserId(String userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<NotificationDto> getUnreadNotificationsByUserId(String userId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public long getUnreadNotificationCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public NotificationDto markAsRead(String notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setRead(true);
        return toDto(notificationRepository.save(n));
    }

    public void markAllAsRead(String userId) {
        List<Notification> list = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }

    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
    }

    private void sendRealtime(String recipientId, NotificationDto dto) {
        try {
            messagingTemplate.convertAndSendToUser(recipientId, "/queue/notifications", dto);
            long unread = getUnreadNotificationCount(recipientId);
            messagingTemplate.convertAndSendToUser(recipientId, "/queue/notification-count", unread);
        } catch (Exception e) {
            System.err.println("Failed to push WS notification: " + e.getMessage());
        }
    }

    private String defaultMessage(NotificationType type, String senderName, String refTitle) {
        return switch (type) {
            case POST_LIKED -> senderName + " liked your post: " + refTitle;
            case POST_COMMENTED -> senderName + " commented on your post: " + refTitle;
            case POST_SAVED -> senderName + " saved your post: " + refTitle;
            case PROPOSAL_RECEIVED -> senderName + " sent you a proposal: " + refTitle;
            case PROPOSAL_ACCEPTED -> senderName + " accepted your proposal: " + refTitle;
            case PROPOSAL_DECLINED -> senderName + " declined your proposal: " + refTitle;
            case DEAL_CONFIRMED -> "Deal confirmed: " + refTitle;
            case DEAL_IN_PROGRESS -> "Deal in progress: " + refTitle;
            case DEAL_COMPLETED -> "Deal completed: " + refTitle;
            case DEAL_CANCELLED -> "Deal cancelled: " + refTitle;
            case RATING_ADDED -> senderName + " left a rating on: " + refTitle;
        };
    }

    private String defaultActionUrl(NotificationType type, String refId) {
        return switch (type) {
            case POST_LIKED, POST_COMMENTED, POST_SAVED -> "/client/my-posts";
            case PROPOSAL_RECEIVED, PROPOSAL_ACCEPTED, PROPOSAL_DECLINED -> "/client/proposals";
            case DEAL_CONFIRMED, DEAL_IN_PROGRESS, DEAL_COMPLETED, DEAL_CANCELLED -> "/client/deals/" + refId;
            case RATING_ADDED -> "/worker/ratings";
        };
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .recipientName(n.getRecipientName())
                .senderId(n.getSenderId())
                .senderName(n.getSenderName())
                .refId(n.getRefId())
                .refTitle(n.getRefTitle())
                .type(n.getType())
                .message(n.getMessage())
                .actionUrl(n.getActionUrl())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

// NotificationDto.java
package com.example.jobup.dto;

import com.example.jobup.entities.NotificationType;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private String id;

    private String recipientId;
    private String recipientName;
    private String senderId;
    private String senderName;

    // Generic reference (proposal/deal/post...)
    private String refId;
    private String refTitle;

    private NotificationType type;
    private String message;
    private String actionUrl;
    private boolean read;

    private Instant createdAt;
}

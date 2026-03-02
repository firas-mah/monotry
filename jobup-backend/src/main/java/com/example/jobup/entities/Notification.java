package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id
    private String id;

    private String recipientId;
    private String recipientName;
    private String senderId;
    private String senderName;

    // Business reference (proposal or deal or post)
    private String refId;      // prefer this generic ref over postId
    private String refTitle;   // e.g., proposal/deal/title when applicable

    private NotificationType type;
    private String message;
    private String actionUrl;

    @Builder.Default
    private boolean read = false;

    @CreatedDate
    private Instant createdAt;
}

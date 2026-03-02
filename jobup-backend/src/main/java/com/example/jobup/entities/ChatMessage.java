package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {
    @Id
    private String id;

    // canonical conversation key (e.g., "clientId:workerId")
    private String chatId;

    // Sender
    private String senderId;
    private String senderName;
    private UserType senderType;   // CLIENT/WORKER

    // Receiver
    private String receiverId;
    private String receiverName;
    private UserType receiverType; // CLIENT/WORKER

    private String content;
    private MessageType messageType;

    // Link to proposal when type == PROPOSAL/PROPOSAL_RESPONSE
    private String proposalId;

    // Link to deal when job state messages are sent
    private String dealId;

    @CreatedDate
    private Instant createdAt;
}

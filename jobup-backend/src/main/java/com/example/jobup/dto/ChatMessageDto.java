// ChatMessageDto.java
package com.example.jobup.dto;

import com.example.jobup.entities.MessageType;
import com.example.jobup.entities.UserType;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private String chatId;

    // Sender
    private String senderId;
    private String senderName;
    private UserType senderType;

    // Receiver
    private String receiverId;
    private String receiverName;
    private UserType receiverType;

    private String content;
    private MessageType messageType;

    // Links
    private String proposalId;
    private String dealId;

    private Instant createdAt;
}

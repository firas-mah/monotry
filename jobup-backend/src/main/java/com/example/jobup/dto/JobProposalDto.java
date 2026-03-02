// JobProposalDto.java
package com.example.jobup.dto;

import com.example.jobup.entities.ProposalStatus;
import com.example.jobup.entities.UserType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProposalDto {
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

    // Canonical roles for downstream logic
    private String clientId;
    private String workerId;

    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private String location;
    private Instant scheduledAt;

    private ProposalStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}

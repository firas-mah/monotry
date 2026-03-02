package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "job_proposals")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobProposal {
    @Id
    private String id;

    private String chatId;

    // Sender info
    private String senderId;
    private String senderName;
    private UserType senderType;    // CLIENT or WORKER

    // Receiver info
    private String receiverId;
    private String receiverName;
    private UserType receiverType;  // CLIENT or WORKER

    // Canonical roles for downstream logic
    private String clientId;
    private String workerId;

    private String title;
    private String description;
    private Integer durationMinutes;     // normalized to minutes
    private BigDecimal price;
    private String location;
    private Instant scheduledAt;

    private ProposalStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "job_deals")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobDeal {
    @Id
    private String id;

    private String proposalId;
    private String chatId;

    private String clientId;
    private String workerId;

    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private String location;
    private Instant scheduledAt;

    private DealStatus status;

    private Instant confirmedAt;
    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

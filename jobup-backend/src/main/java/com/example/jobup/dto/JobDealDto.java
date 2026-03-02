// JobDealDto.java
package com.example.jobup.dto;

import com.example.jobup.entities.DealStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDealDto {
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

    private Instant createdAt;
    private Instant updatedAt;
}

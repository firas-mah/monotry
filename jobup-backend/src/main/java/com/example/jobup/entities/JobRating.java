package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "job_ratings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobRating {
    @Id
    private String id;

    @Indexed(unique = true)
    private String dealId;       // one rating per deal

    private String clientId;     // rater (usually the client)
    private String workerId;     // who is rated

    private int stars;           // 1..5
    private String review;       // <= 1000 chars

    @CreatedDate
    private Instant createdAt;
}

// WorkerRatingStatsDto.java
package com.example.jobup.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerRatingStatsDto {
    private String workerId;
    private double averageRating;
    private long totalRatings;
    private long totalCompletedJobs;
    private RatingDistribution ratingDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private long fiveStars;
        private long fourStars;
        private long threeStars;
        private long twoStars;
        private long oneStar;
    }
}

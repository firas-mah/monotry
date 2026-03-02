package com.example.jobup.services;

import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobDeal;
import com.example.jobup.entities.JobRating;
import com.example.jobup.repositories.JobDealRepository;
import com.example.jobup.repositories.JobRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final JobDealRepository dealRepository;
    private final JobRatingRepository ratingRepository;
    private final WorkerRatingService workerRatingService;

    public boolean canRateDeal(String dealId) {
        JobDeal deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null) {
            return false;
        }

        // Deal must be completed
        if (deal.getStatus() != DealStatus.COMPLETED) {
            return false;
        }

        // Deal must not already have a rating
        return !ratingRepository.existsByDealId(dealId);
    }

    public JobRating addRating(String dealId, String clientId, int stars, String review) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Stars must be 1..5");
        if (review != null && review.length() > 1000) throw new IllegalArgumentException("Review ≤ 1000 chars");

        JobDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new RuntimeException("Deal not found"));

        if (deal.getStatus() != DealStatus.COMPLETED)
            throw new IllegalStateException("Only completed deals can be rated");

        if (ratingRepository.existsByDealId(dealId))
            throw new IllegalStateException("This deal already has a rating");

        JobRating rating = JobRating.builder()
                .dealId(dealId)
                .clientId(clientId)
                .workerId(deal.getWorkerId())
                .stars(stars)
                .review(review)
                .build();

        JobRating saved = ratingRepository.save(rating);

        // Recompute worker aggregate (best-effort)
        try {
            workerRatingService.updateWorkerRating(deal.getWorkerId());
        } catch (Exception ignored) {}

        return saved;
    }
}

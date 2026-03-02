package com.example.jobup.services;

import com.example.jobup.dto.WorkerRatingStatsDto;
import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobDeal;
import com.example.jobup.entities.JobRating;
import com.example.jobup.entities.Worker;
import com.example.jobup.repositories.JobDealRepository;
import com.example.jobup.repositories.JobRatingRepository;
import com.example.jobup.repositories.WorkerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.IntSummaryStatistics;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerRatingService {

    private final JobDealRepository dealRepository;
    private final JobRatingRepository ratingRepository;
    private final WorkerRepo workerRepository;

    public void updateWorkerRating(String workerId) {
        List<JobRating> ratings = ratingRepository.findByWorkerId(workerId);

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        if (ratings.isEmpty()) {
            worker.setRating(0.0);
            worker.setRatingsCount(0);
            workerRepository.save(worker);
            log.info("Reset rating for worker {} to 0 (no ratings)", workerId);
            return;
        }

        IntSummaryStatistics stats = ratings.stream()
                .mapToInt(JobRating::getStars)
                .summaryStatistics();

        double avg = Math.round((stats.getAverage()) * 100.0) / 100.0;
        worker.setRating(avg);
        worker.setRatingsCount(stats.getCount());
        workerRepository.save(worker);

        log.info("Updated rating for worker {}: {} ({} ratings)", workerId, avg, stats.getCount());
    }

    public WorkerRatingStatsDto getWorkerRatingStats(String workerId) {
        List<JobRating> ratings = ratingRepository.findByWorkerId(workerId);
        List<JobDeal> completedDeals = dealRepository.findByWorkerIdAndStatus(workerId, DealStatus.COMPLETED);

        WorkerRatingStatsDto.RatingDistribution dist = WorkerRatingStatsDto.RatingDistribution.builder().build();
        long[] counts = new long[6]; // 1..5

        ratings.forEach(r -> counts[r.getStars()]++);

        dist.setOneStar(counts[1]);
        dist.setTwoStars(counts[2]);
        dist.setThreeStars(counts[3]);
        dist.setFourStars(counts[4]);
        dist.setFiveStars(counts[5]);

        double average = ratings.stream().mapToInt(JobRating::getStars).average().orElse(0.0);
        double rounded = Math.round(average * 100.0) / 100.0;

        return WorkerRatingStatsDto.builder()
                .workerId(workerId)
                .averageRating(rounded)
                .totalRatings(ratings.size())
                .totalCompletedJobs(completedDeals.size())
                .ratingDistribution(dist)
                .build();
    }

    public void updateAllWorkerRatings() {
        List<Worker> workers = workerRepository.findAll();
        log.info("Starting batch update of ratings for {} workers", workers.size());
        for (Worker w : workers) {
            try {
                updateWorkerRating(w.getId());
            } catch (Exception e) {
                log.error("Failed to update rating for worker {}: {}", w.getId(), e.getMessage());
            }
        }
        log.info("Completed batch update of worker ratings");
    }
}

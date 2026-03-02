package com.example.jobup.controller;

import com.example.jobup.dto.JobDealDto;
import com.example.jobup.dto.WorkerRatingStatsDto;
import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobRating;
import com.example.jobup.services.JobDealService;
import com.example.jobup.services.RatingService;
import com.example.jobup.services.WorkerRatingService;

import java.security.Principal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobDealController {

    private final JobDealService dealService;
    private final WorkerRatingService workerRatingService;
    private final RatingService ratingService;

    @PostMapping("/from-proposal/{proposalId}")
    public ResponseEntity<JobDealDto> createDealFromProposal(@PathVariable String proposalId) {
        return ResponseEntity.ok(dealService.createDealFromProposal(proposalId));
    }

    @PutMapping("/{dealId}/status")
    public ResponseEntity<JobDealDto> updateDealStatus(
            @PathVariable String dealId,
            @RequestBody UpdateStatusRequest req
    ) {
        return ResponseEntity.ok(dealService.updateDealStatus(dealId, req.getStatus()));
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<JobDealDto>> getDealsByChatId(@PathVariable String chatId) {
        return ResponseEntity.ok(dealService.getDealsByChatId(chatId));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<JobDealDto>> getDealsByWorkerId(@PathVariable String workerId) {
        return ResponseEntity.ok(dealService.getDealsByWorkerId(workerId));
    }

    @GetMapping("/worker/{workerId}/completed")
    public ResponseEntity<List<JobDealDto>> getCompletedDealsByWorkerId(@PathVariable String workerId) {
        return ResponseEntity.ok(dealService.getCompletedDealsByWorkerId(workerId));
    }

    @GetMapping("/worker/{workerId}/rating-stats")
    public ResponseEntity<WorkerRatingStatsDto> getWorkerRatingStats(@PathVariable String workerId) {
        return ResponseEntity.ok(workerRatingService.getWorkerRatingStats(workerId));
    }

    @GetMapping("/{dealId}/can-rate")
    public ResponseEntity<Boolean> canRateDeal(@PathVariable String dealId) {
        boolean canRate = ratingService.canRateDeal(dealId);
        return ResponseEntity.ok(canRate);
    }

    @PostMapping("/{dealId}/rating")
    public ResponseEntity<JobRating> addRating(
            @PathVariable String dealId,
            @RequestBody RatingRequest request,
            Principal principal
    ) {
        String clientId = principal.getName(); // Get the authenticated user ID
        JobRating rating = ratingService.addRating(dealId, clientId, request.getRating(), request.getReview());
        return ResponseEntity.ok(rating);
    }

    // Rating endpoints moved to RatingController

    @Getter @Setter
    public static class UpdateStatusRequest {
        private DealStatus status;
    }

    @Getter @Setter
    public static class RatingRequest {
        private int rating;    // 1-5 stars
        private String review; // optional review text
    }
}

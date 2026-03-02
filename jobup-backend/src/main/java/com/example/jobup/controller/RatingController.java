package com.example.jobup.controller;

import com.example.jobup.entities.JobRating;
import com.example.jobup.services.RatingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<JobRating> addRating(@RequestBody CreateRatingRequest req) {
        JobRating rating = ratingService.addRating(req.getDealId(), req.getClientId(), req.getStars(), req.getReview());
        return ResponseEntity.ok(rating);
    }

    @Getter @Setter
    public static class CreateRatingRequest {
        private String dealId;
        private String clientId;
        private int stars;       // 1..5
        private String review;   // optional, <= 1000
    }
}

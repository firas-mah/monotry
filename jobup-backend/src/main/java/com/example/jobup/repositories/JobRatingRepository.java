package com.example.jobup.repositories;

import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobDeal;
import com.example.jobup.entities.JobRating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRatingRepository extends MongoRepository<JobRating, String>{
    boolean existsByDealId(String dealId);
    List<JobRating> findByWorkerId(String workerId);
    Optional<JobRating> findByDealId(String dealId);

}

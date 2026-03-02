package com.example.jobup.repositories;

import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobDeal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobDealRepository extends MongoRepository<JobDeal, String> {
    // Check if a specific deal already has a rating

    java.util.Optional<JobDeal> findByProposalId(String proposalId);
    List<JobDeal> findByChatId(String chatId);
    List<JobDeal> findByWorkerId(String workerId);
    List<JobDeal> findByWorkerIdAndStatus(String workerId, DealStatus status);
    int countByWorkerIdAndStatus(String workerId, DealStatus status);

}
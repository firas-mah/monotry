package com.example.jobup.repositories;

import com.example.jobup.entities.JobProposal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobProposalRepository extends MongoRepository<JobProposal, String> {
    List<JobProposal> findByChatIdOrderByCreatedAtDesc(String chatId);
    List<JobProposal> findByWorkerIdOrderByCreatedAtDesc(String workerId);
    List<JobProposal> findByClientIdOrderByCreatedAtDesc(String clientId);

}
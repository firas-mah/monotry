package com.example.jobup.repositories;

import com.example.jobup.entities.JobPost;
import io.micrometer.common.KeyValues;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostRepository extends MongoRepository<JobPost, String> {
    List<JobPost> findByCreatedByIdOrderByCreatedAtDesc(String createdById);
    List<JobPost> findBySavedByContains(String userId, Sort sort);
}
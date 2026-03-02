package com.example.jobup.repositories;

import com.example.jobup.entities.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String userId);
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(String userId);
    long countByRecipientIdAndReadFalse(String userId);

}

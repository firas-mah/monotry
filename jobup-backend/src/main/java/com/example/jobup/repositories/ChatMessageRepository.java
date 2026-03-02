package com.example.jobup.repositories;

import com.example.jobup.entities.ChatMessage;
import com.example.jobup.entities.UserType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(String chatId);
    List<ChatMessage> findByReceiverIdOrderByCreatedAtDesc(String receiverId);
    List<ChatMessage> findByReceiverIdAndReceiverTypeOrderByCreatedAtDesc(String receiverId, UserType receiverType);

}
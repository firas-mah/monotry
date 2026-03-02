package com.example.jobup.services;

import com.example.jobup.entities.ChatMessage;
import com.example.jobup.entities.JobProposal;
import com.example.jobup.entities.UserType;
import com.example.jobup.repositories.ChatMessageRepository;
import com.example.jobup.repositories.JobProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("migrate") // <-- run only when you start with `--spring.profiles.active=migrate` (optional but recommended)
public class MigrationService implements CommandLineRunner {

    private final JobProposalRepository proposalRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    public void run(String... args) {
        log.info("Starting database migration...");
        int p = migrateProposals();
        int m = migrateChatMessages();
        log.info("Database migration completed. Proposals updated: {}, Messages updated: {}", p, m);
    }

    private int migrateProposals() {
        List<JobProposal> proposals = proposalRepository.findAll();
        int migratedCount = 0;

        for (JobProposal proposal : proposals) {
            boolean changed = false;

            String[] ids = parseChatId(proposal.getChatId());
            if (ids == null) continue;
            String clientIdFromChat = ids[0];
            String workerIdFromChat = ids[1];

            // Fill canonical roles if missing
            if (proposal.getClientId() == null) {
                proposal.setClientId(clientIdFromChat);
                changed = true;
            }
            if (proposal.getWorkerId() == null) {
                proposal.setWorkerId(workerIdFromChat);
                changed = true;
            }

            // Fill receiver info if missing
            if (proposal.getReceiverId() == null || proposal.getReceiverType() == null) {
                if (proposal.getSenderType() == UserType.CLIENT) {
                    proposal.setReceiverId(workerIdFromChat);
                    if (proposal.getReceiverType() == null) proposal.setReceiverType(UserType.WORKER);
                    if (proposal.getReceiverName() == null) proposal.setReceiverName("Worker");
                } else if (proposal.getSenderType() == UserType.WORKER) {
                    proposal.setReceiverId(clientIdFromChat);
                    if (proposal.getReceiverType() == null) proposal.setReceiverType(UserType.CLIENT);
                    if (proposal.getReceiverName() == null) proposal.setReceiverName("Client");
                }
                changed = true;
            }

            if (changed) {
                proposalRepository.save(proposal);
                migratedCount++;
                log.info("Migrated proposal {}", proposal.getId());
            }
        }
        return migratedCount;
    }

    private int migrateChatMessages() {
        List<ChatMessage> messages = messageRepository.findAll();
        int migratedCount = 0;

        for (ChatMessage message : messages) {
            boolean changed = false;

            if (message.getReceiverId() == null || message.getReceiverType() == null) {
                String[] ids = parseChatId(message.getChatId());
                if (ids == null) continue;
                String clientIdFromChat = ids[0];
                String workerIdFromChat = ids[1];

                if (message.getSenderType() == UserType.CLIENT) {
                    message.setReceiverId(workerIdFromChat);
                    if (message.getReceiverType() == null) message.setReceiverType(UserType.WORKER);
                    if (message.getReceiverName() == null) message.setReceiverName("Worker");
                } else if (message.getSenderType() == UserType.WORKER) {
                    message.setReceiverId(clientIdFromChat);
                    if (message.getReceiverType() == null) message.setReceiverType(UserType.CLIENT);
                    if (message.getReceiverName() == null) message.setReceiverName("Client");
                }
                changed = true;
            }

            if (changed) {
                messageRepository.save(message);
                migratedCount++;
                log.info("Migrated chat message {}", message.getId());
            }
        }
        return migratedCount;
    }

    /**
     * Accepts "client_worker" or "client:worker".
     */
    private String[] parseChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) return null;
        String[] parts = chatId.split("[:_]");
        if (parts.length != 2) {
            log.warn("Unexpected chatId format: {}", chatId);
            return null;
        }
        return parts;
    }
}

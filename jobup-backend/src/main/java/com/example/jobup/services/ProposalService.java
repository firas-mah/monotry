package com.example.jobup.services;

import com.example.jobup.dto.JobProposalDto;
import com.example.jobup.entities.JobProposal;
import com.example.jobup.entities.ProposalStatus;
import com.example.jobup.entities.UserType;
import com.example.jobup.repositories.JobProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final JobProposalRepository proposalRepository;

    public JobProposalDto createProposal(
            String chatId,
            String senderId, String senderName, UserType senderType,
            String receiverId, String receiverName, UserType receiverType,
            String title, String description,
            Integer durationMinutes, BigDecimal price,
            String location, Instant scheduledAt
    ) {
        // Derive canonical client/worker from sender/receiver types (safety net)
        String clientId = senderType == UserType.CLIENT ? senderId
                : receiverType == UserType.CLIENT ? receiverId : null;
        String workerId = senderType == UserType.WORKER ? senderId
                : receiverType == UserType.WORKER ? receiverId : null;

        JobProposal proposal = JobProposal.builder()
                .chatId(chatId)
                .senderId(senderId)
                .senderName(senderName)
                .senderType(senderType)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .receiverType(receiverType)
                .clientId(clientId)
                .workerId(workerId)
                .title(title)
                .description(description)
                .durationMinutes(durationMinutes)
                .price(price)
                .location(location)
                .scheduledAt(scheduledAt)
                .status(ProposalStatus.PENDING)
                // createdAt/updatedAt via auditing
                .build();

        JobProposal saved = proposalRepository.save(proposal);
        return toDto(saved);
    }

    public JobProposalDto updateProposalStatus(String proposalId, ProposalStatus status) {
        JobProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // Allowed transitions: PENDING/NEGOTIATED -> ACCEPTED/DECLINED/NEGOTIATED
        boolean ok =
                (proposal.getStatus() == ProposalStatus.PENDING && (status == ProposalStatus.ACCEPTED || status == ProposalStatus.DECLINED || status == ProposalStatus.NEGOTIATED))
                        || (proposal.getStatus() == ProposalStatus.NEGOTIATED && (status == ProposalStatus.ACCEPTED || status == ProposalStatus.DECLINED || status == ProposalStatus.NEGOTIATED));

        if (!ok) throw new IllegalStateException("Invalid proposal transition: " + proposal.getStatus() + " → " + status);

        proposal.setStatus(status);
        JobProposal saved = proposalRepository.save(proposal);
        return toDto(saved);
    }

    public List<JobProposalDto> getProposalsByChatId(String chatId) {
        return proposalRepository.findByChatIdOrderByCreatedAtDesc(chatId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<JobProposalDto> getProposalsByWorkerId(String workerId) {
        return proposalRepository.findByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<JobProposalDto> getProposalsByClientId(String clientId) {
        return proposalRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private JobProposalDto toDto(JobProposal p) {
        return JobProposalDto.builder()
                .id(p.getId())
                .chatId(p.getChatId())
                .senderId(p.getSenderId())
                .senderName(p.getSenderName())
                .senderType(p.getSenderType())
                .receiverId(p.getReceiverId())
                .receiverName(p.getReceiverName())
                .receiverType(p.getReceiverType())
                .clientId(p.getClientId())
                .workerId(p.getWorkerId())
                .title(p.getTitle())
                .description(p.getDescription())
                .durationMinutes(p.getDurationMinutes())
                .price(p.getPrice())
                .location(p.getLocation())
                .scheduledAt(p.getScheduledAt())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

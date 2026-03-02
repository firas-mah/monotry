package com.example.jobup.services;

import com.example.jobup.dto.JobDealDto;
import com.example.jobup.entities.DealStatus;
import com.example.jobup.entities.JobDeal;
import com.example.jobup.entities.JobProposal;
import com.example.jobup.entities.UserType;
import com.example.jobup.repositories.JobDealRepository;
import com.example.jobup.repositories.JobProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobDealService {

    private final JobDealRepository dealRepository;
    private final JobProposalRepository proposalRepository;

    public JobDealDto createDealFromProposal(String proposalId) {
        JobProposal p = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (p.getStatus() != com.example.jobup.entities.ProposalStatus.ACCEPTED) {
            throw new IllegalStateException("Proposal must be ACCEPTED");
        }

        // Idempotent: if a deal already exists for this proposal, return it
        JobDeal existing = dealRepository.findByProposalId(proposalId).orElse(null);
        if (existing != null) {
            return toDto(existing);
        }

        // Ensure canonical roles are set (safety net)
        String clientId = p.getClientId();
        String workerId = p.getWorkerId();
        if (clientId == null || workerId == null) {
            clientId = p.getSenderType() == UserType.CLIENT ? p.getSenderId() : p.getReceiverId();
            workerId = p.getSenderType() == UserType.WORKER ? p.getSenderId() : p.getReceiverId();
        }

        JobDeal deal = JobDeal.builder()
                .proposalId(p.getId())
                .chatId(p.getChatId())
                .clientId(clientId)
                .workerId(workerId)
                .title(p.getTitle())
                .description(p.getDescription())
                .durationMinutes(p.getDurationMinutes())
                .price(p.getPrice())
                .location(p.getLocation())
                .scheduledAt(p.getScheduledAt())
                .status(DealStatus.CONFIRMED)
                .confirmedAt(Instant.now())
                .build();

        JobDeal saved = dealRepository.save(deal);
        return toDto(saved);
    }

    public JobDealDto updateDealStatus(String dealId, DealStatus newStatus) {
        JobDeal d = dealRepository.findById(dealId)
                .orElseThrow(() -> new RuntimeException("Deal not found"));

        boolean ok =
                (d.getStatus() == DealStatus.CONFIRMED && (newStatus == DealStatus.IN_PROGRESS || newStatus == DealStatus.CANCELLED)) ||
                        (d.getStatus() == DealStatus.IN_PROGRESS && (newStatus == DealStatus.COMPLETED || newStatus == DealStatus.CANCELLED));

        if (!ok) throw new IllegalStateException("Invalid transition: " + d.getStatus() + " → " + newStatus);

        d.setStatus(newStatus);
        if (newStatus == DealStatus.COMPLETED) {
            d.setCompletedAt(Instant.now());
        }

        JobDeal saved = dealRepository.save(d);
        return toDto(saved);
    }

    public List<JobDealDto> getDealsByChatId(String chatId) {
        return dealRepository.findByChatId(chatId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<JobDealDto> getDealsByWorkerId(String workerId) {
        return dealRepository.findByWorkerId(workerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<JobDealDto> getCompletedDealsByWorkerId(String workerId) {
        return dealRepository.findByWorkerIdAndStatus(workerId, DealStatus.COMPLETED)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private JobDealDto toDto(JobDeal d) {
        return JobDealDto.builder()
                .id(d.getId())
                .proposalId(d.getProposalId())
                .chatId(d.getChatId())
                .clientId(d.getClientId())
                .workerId(d.getWorkerId())
                .title(d.getTitle())
                .description(d.getDescription())
                .durationMinutes(d.getDurationMinutes())
                .price(d.getPrice())
                .location(d.getLocation())
                .scheduledAt(d.getScheduledAt())
                .status(d.getStatus())
                .confirmedAt(d.getConfirmedAt())
                .completedAt(d.getCompletedAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

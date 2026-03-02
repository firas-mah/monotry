package com.example.jobup.controller;

import com.example.jobup.dto.JobProposalDto;
import com.example.jobup.entities.ProposalStatus;
import com.example.jobup.entities.UserType;
import com.example.jobup.services.JobDealService;
import com.example.jobup.services.ProposalService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProposalController {

    private final ProposalService proposalService;
    private final JobDealService jobDealService;

    @PostMapping
    public ResponseEntity<JobProposalDto> createProposal(@RequestBody CreateProposalRequest req) {
        JobProposalDto dto = proposalService.createProposal(
                req.getChatId(),
                req.getSenderId(), req.getSenderName(), req.getSenderType(),
                req.getReceiverId(), req.getReceiverName(), req.getReceiverType(),
                req.getTitle(), req.getDescription(),
                req.getDurationMinutes(), req.getPrice(),
                req.getLocation(), req.getScheduledAt()
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{proposalId}/status")
    public ResponseEntity<JobProposalDto> updateProposalStatus(
            @PathVariable String proposalId,
            @RequestBody UpdateStatusRequest req
    ) {
        JobProposalDto dto = proposalService.updateProposalStatus(proposalId, req.getStatus());

        // If accepted → auto create deal (idempotent in service)
        if (dto.getStatus() == ProposalStatus.ACCEPTED) {
            jobDealService.createDealFromProposal(proposalId);
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<JobProposalDto>> getProposalsByChatId(@PathVariable String chatId) {
        return ResponseEntity.ok(proposalService.getProposalsByChatId(chatId));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<JobProposalDto>> getProposalsByWorkerId(@PathVariable String workerId) {
        return ResponseEntity.ok(proposalService.getProposalsByWorkerId(workerId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<JobProposalDto>> getProposalsByClientId(@PathVariable String clientId) {
        return ResponseEntity.ok(proposalService.getProposalsByClientId(clientId));
    }

    @Getter @Setter
    public static class CreateProposalRequest {
        private String chatId;

        private String senderId;
        private String senderName;
        private UserType senderType;

        private String receiverId;
        private String receiverName;
        private UserType receiverType;

        private String title;
        private String description;
        private Integer durationMinutes;
        private BigDecimal price;
        private String location;
        private Instant scheduledAt;
    }

    @Getter @Setter
    public static class UpdateStatusRequest {
        private ProposalStatus status;
    }
}

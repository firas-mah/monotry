package com.example.jobup.controller;

import com.example.jobup.dto.ChatMessageDto;
import com.example.jobup.dto.JobProposalDto;
import com.example.jobup.entities.MessageType;
import com.example.jobup.entities.UserType;
import com.example.jobup.services.ChatService;
import com.example.jobup.services.ProposalService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ChatService chatService;
    private final ProposalService proposalService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public ChatMessageDto sendMessage(@Payload WebSocketMessage msg, SimpMessageHeaderAccessor headers) {
        String chatId = msg.getChatId();

        ChatMessageDto dto = chatService.sendMessage(
                chatId,
                msg.getSenderId(), msg.getSenderName(), msg.getSenderType(),
                msg.getReceiverId(), msg.getReceiverName(), msg.getReceiverType(),
                msg.getContent(), MessageType.TEXT,
                null, null
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, dto);
        return dto;
    }

    @MessageMapping("/chat.sendProposal")
    public JobProposalDto sendProposal(@Payload WebSocketProposalMessage pm, SimpMessageHeaderAccessor headers) {
        String chatId = pm.getChatId();

        JobProposalDto proposal = proposalService.createProposal(
                chatId,
                pm.getSenderId(), pm.getSenderName(), pm.getSenderType(),
                pm.getReceiverId(), pm.getReceiverName(), pm.getReceiverType(),
                pm.getTitle(), pm.getDescription(),
                pm.getDurationMinutes(), pm.getPrice(),
                pm.getLocation(), pm.getScheduledAt()
        );

        // Also send a PROPOSAL message linking proposalId
        chatService.sendMessage(
                chatId,
                pm.getSenderId(), pm.getSenderName(), pm.getSenderType(),
                pm.getReceiverId(), pm.getReceiverName(), pm.getReceiverType(),
                "Sent a proposal: " + proposal.getTitle(),
                MessageType.PROPOSAL,
                proposal.getId(), // link
                null
        );

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, proposal);
        return proposal;
    }

    @MessageMapping("/chat.addUser")
    public ChatMessageDto addUser(@Payload WebSocketMessage msg, SimpMessageHeaderAccessor headers) {
        headers.getSessionAttributes().put("username", msg.getSenderName());
        headers.getSessionAttributes().put("chatId", msg.getChatId());

        ChatMessageDto join = chatService.sendMessage(
                msg.getChatId(),
                msg.getSenderId(), msg.getSenderName(), msg.getSenderType(),
                msg.getReceiverId(), msg.getReceiverName(), msg.getReceiverType(),
                msg.getSenderName() + " joined the chat",
                MessageType.TEXT,
                null, null
        );
        messagingTemplate.convertAndSend("/topic/chat/" + msg.getChatId(), join);
        return join;
    }

    @Getter @Setter
    public static class WebSocketMessage {
        private String chatId;
        private String senderId;
        private String senderName;
        private UserType senderType;
        private String receiverId;
        private String receiverName;
        private UserType receiverType;
        private String content;
    }

    @Getter @Setter
    public static class WebSocketProposalMessage {
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
}

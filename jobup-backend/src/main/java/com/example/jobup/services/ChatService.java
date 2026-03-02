package com.example.jobup.services;

import com.example.jobup.dto.ChatMessageDto;
import com.example.jobup.entities.ChatMessage;
import com.example.jobup.entities.MessageType;
import com.example.jobup.entities.UserType;
import com.example.jobup.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessageDto> getChatMessages(String chatId) {
        // NOTE: repo method should sort by createdAt
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ChatMessageDto sendMessage(
            String chatId,
            String senderId, String senderName, UserType senderType,
            String receiverId, String receiverName, UserType receiverType,
            String content, MessageType messageType,
            String proposalId, String dealId // optional links
    ) {
        ChatMessage message = ChatMessage.builder()
                .chatId(chatId)
                .senderId(senderId)
                .senderName(senderName)
                .senderType(senderType)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .receiverType(receiverType)
                .content(content)
                .messageType(messageType)
                .proposalId(proposalId)
                .dealId(dealId)
                // createdAt is auto via @CreatedDate
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return toDto(saved);
    }

    public List<ChatMessageDto> getMessagesByReceiverId(String receiverId) {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ChatMessageDto> getMessagesByReceiverIdAndType(String receiverId, UserType receiverType) {
        List<ChatMessage> messages = chatMessageRepository
                .findByReceiverIdAndReceiverTypeOrderByCreatedAtDesc(receiverId, receiverType);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .chatId(m.getChatId())
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .senderType(m.getSenderType())
                .receiverId(m.getReceiverId())
                .receiverName(m.getReceiverName())
                .receiverType(m.getReceiverType())
                .content(m.getContent())
                .messageType(m.getMessageType())
                .proposalId(m.getProposalId())
                .dealId(m.getDealId())
                .createdAt(m.getCreatedAt())
                .build();
    }
}

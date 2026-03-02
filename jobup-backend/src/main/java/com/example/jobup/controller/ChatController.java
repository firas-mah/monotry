package com.example.jobup.controller;

import com.example.jobup.dto.ChatMessageDto;
import com.example.jobup.entities.MessageType;
import com.example.jobup.entities.UserType;
import com.example.jobup.services.ChatService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @GetMapping(value = "/{chatId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(@PathVariable String chatId) {
        return ResponseEntity.ok(chatService.getChatMessages(chatId));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable String chatId,
            @RequestBody SendMessageRequest req
    ) {
        ChatMessageDto dto = chatService.sendMessage(
                chatId,
                req.getSenderId(), req.getSenderName(), req.getSenderType(),
                req.getReceiverId(), req.getReceiverName(), req.getReceiverType(),
                req.getContent(), MessageType.TEXT,
                null, null // proposalId, dealId (not a proposal message)
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping(value = "/receiver/{receiverId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatMessageDto>> getMessagesByReceiverId(@PathVariable String receiverId) {
        return ResponseEntity.ok(chatService.getMessagesByReceiverId(receiverId));
    }

    @GetMapping(value = "/receiver/{receiverId}/type/{receiverType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatMessageDto>> getMessagesByReceiverIdAndType(
            @PathVariable String receiverId,
            @PathVariable UserType receiverType
    ) {
        return ResponseEntity.ok(chatService.getMessagesByReceiverIdAndType(receiverId, receiverType));
    }

    @Getter @Setter
    public static class SendMessageRequest {
        private String senderId;
        private String senderName;
        private UserType senderType;
        private String receiverId;
        private String receiverName;
        private UserType receiverType;
        private String content;
    }
}

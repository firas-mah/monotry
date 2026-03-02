package com.example.jobup.config;

import com.example.jobup.repositories.UserRepository;
import com.example.jobup.services.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);

                // map username -> userId (fallback to username if not found)
                String userId = userRepository.findByUsername(username)
                        .map(u -> u.getId())
                        .orElse(username);

                // set Principal name to userId so convertAndSendToUser(recipientId, ...) matches
                accessor.setUser(() -> userId);
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        // Prefer standard Authorization header
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        }
        // Fallbacks
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isBlank()) return token;

        String query = accessor.getFirstNativeHeader("query");
        if (query != null && !query.isBlank()) return extractTokenFromQuery(query);

        return null;
    }

    private String extractTokenFromQuery(String query) {
        for (String p : query.split("&")) {
            if (p.startsWith("token=")) return p.substring(6);
        }
        return null;
    }
} 
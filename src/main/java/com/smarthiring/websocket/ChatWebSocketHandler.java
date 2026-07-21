package com.smarthiring.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.model.ChatMessage;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.security.JwtService;
import com.smarthiring.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, Set<WebSocketSession>> sessionsByShift = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
            JwtService jwtService,
            UserRepository userRepository,
            ChatService chatService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
        String token = params.get("token");
        Long shiftId = parseLong(params.get("shiftId"));
        if (token == null || shiftId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !chatService.canAccessShift(shiftId, user)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        session.getAttributes().put("shiftId", shiftId);
        sessionsByShift.computeIfAbsent(shiftId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long shiftId = (Long) session.getAttributes().get("shiftId");
        if (shiftId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByShift.get(shiftId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public void broadcast(ChatMessage chatMessage) {
        if (chatMessage.getShift() == null || chatMessage.getShift().getId() == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByShift.get(chatMessage.getShift().getId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(toPayload(chatMessage)));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception ignored) {
            // REST still persists the message; a failed socket broadcast should not break chat history.
        }
    }

    private Map<String, Object> toPayload(ChatMessage chatMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", chatMessage.getId());
        payload.put("message", chatMessage.getMessage());
        payload.put("createdAt", chatMessage.getCreatedAt() == null ? null : chatMessage.getCreatedAt().toString());

        Map<String, Object> sender = new LinkedHashMap<>();
        if (chatMessage.getSender() != null) {
            sender.put("id", chatMessage.getSender().getId());
            sender.put("name", chatMessage.getSender().getName());
            sender.put("role", chatMessage.getSender().getRole());
        }
        payload.put("sender", sender);

        Map<String, Object> shift = new LinkedHashMap<>();
        if (chatMessage.getShift() != null) {
            shift.put("id", chatMessage.getShift().getId());
            shift.put("title", chatMessage.getShift().getTitle());
        }
        payload.put("shift", shift);

        return payload;
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

package com.smarthiring.controller;

import com.smarthiring.dto.ChatMessageRequest;
import com.smarthiring.model.ChatMessage;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.ChatService;
import com.smarthiring.websocket.ChatWebSocketHandler;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final UserRepository userRepository;

    public ChatController(ChatService chatService, ChatWebSocketHandler chatWebSocketHandler, UserRepository userRepository) {
        this.chatService = chatService;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.userRepository = userRepository;
    }

    @GetMapping("/shifts/{shiftId}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long shiftId, Authentication authentication) {
        return chatService.getMessages(shiftId, currentUser(authentication));
    }

    @PostMapping("/shifts/{shiftId}/messages")
    public ChatMessage sendMessage(
            @PathVariable Long shiftId,
            @RequestBody ChatMessageRequest request,
            Authentication authentication
    ) {
        ChatMessage saved = chatService.sendMessage(shiftId, request, currentUser(authentication));
        chatWebSocketHandler.broadcast(saved);
        return saved;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

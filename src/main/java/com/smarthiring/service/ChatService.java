package com.smarthiring.service;

import com.smarthiring.dto.ChatMessageRequest;
import com.smarthiring.model.Application;
import com.smarthiring.model.ChatMessage;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ChatMessageRepository;
import com.smarthiring.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ShiftRepository shiftRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository
    ) {
        this(chatMessageRepository, shiftRepository, applicationRepository, null);
    }

    @Autowired
    public ChatService(
            ChatMessageRepository chatMessageRepository,
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            NotificationService notificationService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.shiftRepository = shiftRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    public List<ChatMessage> getMessages(Long shiftId, User currentUser) {
        Shift shift = findAccessibleShift(shiftId, currentUser);
        return chatMessageRepository.findAllByShiftIdOrderByCreatedAtAsc(shift.getId());
    }

    public ChatMessage sendMessage(Long shiftId, ChatMessageRequest request, User currentUser) {
        if (request.getMessage() == null || request.getMessage().trim().length() < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Message is required");
        }

        Shift shift = findAccessibleShift(shiftId, currentUser);
        Application acceptedApplication = getAcceptedApplication(shift)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Chat is available after a worker is accepted"));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setShift(shift);
        chatMessage.setApplication(acceptedApplication);
        chatMessage.setSender(currentUser);
        chatMessage.setMessage(request.getMessage().trim());
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        notifyOtherParticipant(saved);
        return saved;
    }

    public boolean canAccessShift(Long shiftId, User currentUser) {
        try {
            findAccessibleShift(shiftId, currentUser);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    private Shift findAccessibleShift(Long shiftId, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return shift;
        }

        boolean ownsShift = "MANAGER".equalsIgnoreCase(currentUser.getRole())
                && shift.getManager() != null
                && shift.getManager().getId().equals(currentUser.getId());

        boolean acceptedWorker = getAcceptedApplication(shift)
                .map(application -> application.getWorker() != null && application.getWorker().getId().equals(currentUser.getId()))
                .orElse(false);

        if (ownsShift || acceptedWorker) {
            return shift;
        }

        throw new ResponseStatusException(FORBIDDEN, "Chat is only available to the shift manager and accepted worker");
    }

    private java.util.Optional<Application> getAcceptedApplication(Shift shift) {
        if (shift == null || shift.getId() == null) {
            return java.util.Optional.empty();
        }

        return applicationRepository.findAllByShiftId(shift.getId()).stream()
                .filter(application -> "ACCEPTED".equalsIgnoreCase(application.getStatus()))
                .findFirst();
    }

    private void notifyOtherParticipant(ChatMessage message) {
        if (notificationService == null || message.getShift() == null || message.getSender() == null) {
            return;
        }
        Shift shift = message.getShift();
        User recipient = "MANAGER".equalsIgnoreCase(message.getSender().getRole())
                ? shift.getAssignedWorker()
                : shift.getManager();
        if (recipient == null) {
            return;
        }
        notificationService.create(
                recipient,
                "CHAT_MESSAGE",
                "New chat message",
                "You have a new message about %s.".formatted(shift.getTitle()),
                "/shift-chat/" + shift.getId(),
                false,
                "chat-message:%d".formatted(message.getId())
        );
    }
}

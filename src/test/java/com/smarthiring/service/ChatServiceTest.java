package com.smarthiring.service;

import com.smarthiring.dto.ChatMessageRequest;
import com.smarthiring.model.Application;
import com.smarthiring.model.ChatMessage;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ChatMessageRepository;
import com.smarthiring.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final ChatService chatService = new ChatService(chatMessageRepository, shiftRepository, applicationRepository);

    @Test
    void acceptedWorkerCanSendMessage() {
        User worker = worker(1L);
        Shift shift = shift(manager(2L), worker);
        Application application = acceptedApplication(worker, shift);
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("I can make this shift.");

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(applicationRepository.findAllByShiftId(shift.getId())).thenReturn(List.of(application));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage saved = chatService.sendMessage(shift.getId(), request, worker);

        assertThat(saved.getShift()).isEqualTo(shift);
        assertThat(saved.getApplication()).isEqualTo(application);
        assertThat(saved.getSender()).isEqualTo(worker);
        assertThat(saved.getMessage()).isEqualTo("I can make this shift.");
    }

    @Test
    void owningManagerCanReadMessages() {
        User manager = manager(2L);
        User worker = worker(1L);
        Shift shift = shift(manager, worker);
        ChatMessage chatMessage = new ChatMessage();

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(applicationRepository.findAllByShiftId(shift.getId())).thenReturn(List.of(acceptedApplication(worker, shift)));
        when(chatMessageRepository.findAllByShiftIdOrderByCreatedAtAsc(shift.getId())).thenReturn(List.of(chatMessage));

        List<ChatMessage> messages = chatService.getMessages(shift.getId(), manager);

        assertThat(messages).containsExactly(chatMessage);
    }

    @Test
    void unrelatedWorkerCannotAccessChat() {
        User acceptedWorker = worker(1L);
        User otherWorker = worker(9L);
        Shift shift = shift(manager(2L), acceptedWorker);

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(applicationRepository.findAllByShiftId(shift.getId())).thenReturn(List.of(acceptedApplication(acceptedWorker, shift)));

        assertThatThrownBy(() -> chatService.getMessages(shift.getId(), otherWorker))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    private User worker(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("WORKER");
        user.setStatus("ACTIVE");
        return user;
    }

    private User manager(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("MANAGER");
        user.setStatus("ACTIVE");
        return user;
    }

    private Shift shift(User manager, User worker) {
        Shift shift = new Shift();
        shift.setId(10L);
        shift.setManager(manager);
        shift.setAssignedWorker(worker);
        shift.setStatus("COMPLETED");
        return shift;
    }

    private Application acceptedApplication(User worker, Shift shift) {
        Application application = new Application();
        application.setId(20L);
        application.setWorker(worker);
        application.setShift(shift);
        application.setStatus("ACCEPTED");
        return application;
    }
}

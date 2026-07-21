package com.smarthiring.repository;

import com.smarthiring.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByShiftIdOrderByCreatedAtAsc(Long shiftId);
}

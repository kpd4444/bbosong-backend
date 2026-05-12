package com.posong.ai_laundry.domain.chat.repository;

import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

	void deleteAllByChatRoom(ChatRoom chatRoom);
}

package com.posong.ai_laundry.domain.chat.repository;

import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByMember_MemberId(Long memberId);
}

package com.posong.ai_laundry.domain.chat.entity;

import com.posong.ai_laundry.domain.chat.constant.MessageSenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_message_id")
	private Long chatMessageId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_room_id", nullable = false)
	private ChatRoom chatRoom;

	@Enumerated(EnumType.STRING)
	@Column(name = "sender_type", nullable = false, length = 20)
	private MessageSenderType senderType;

	@Column(nullable = false, length = 2000)
	private String content;

	@Column(name = "image_key", length = 500)
	private String imageKey;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Builder
	private ChatMessage(ChatRoom chatRoom, MessageSenderType senderType, String content, String imageKey) {
		this.chatRoom = chatRoom;
		this.senderType = senderType;
		this.content = content;
		this.imageKey = imageKey;
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}

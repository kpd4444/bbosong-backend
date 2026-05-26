package com.posong.ai_laundry.domain.chat.mapper;

import com.posong.ai_laundry.domain.chat.constant.MessageSenderType;
import com.posong.ai_laundry.domain.chat.dto.ChatMessageResDto;
import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import com.posong.ai_laundry.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMapper {

	private final ImageUrlResolver imageUrlResolver;

	public ChatMessage toUserMessage(ChatRoom chatRoom, String content, String imageKey) {
		return ChatMessage.builder()
				.chatRoom(chatRoom)
				.senderType(MessageSenderType.USER)
				.content(content)
				.imageKey(imageKey)
				.build();
	}

	public ChatMessage toAssistantMessage(ChatRoom chatRoom, String content) {
		return ChatMessage.builder()
				.chatRoom(chatRoom)
				.senderType(MessageSenderType.ASSISTANT)
				.content(content)
				.build();
	}

	public ChatMessageResDto toChatMessageResDto(ChatMessage chatMessage) {
		return new ChatMessageResDto(
				chatMessage.getChatMessageId(),
				chatMessage.getSenderType(),
				chatMessage.getContent(),
				imageUrlResolver.resolve(chatMessage.getImageKey()),
				chatMessage.getCreatedAt()
		);
	}
}

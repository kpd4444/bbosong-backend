package com.posong.ai_laundry.domain.chat.mapper;

import com.posong.ai_laundry.domain.chat.constant.MessageSenderType;
import com.posong.ai_laundry.domain.chat.dto.ChatMessageResDto;
import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

	public ChatMessage toUserMessage(ChatRoom chatRoom, String content) {
		return ChatMessage.builder()
				.chatRoom(chatRoom)
				.senderType(MessageSenderType.USER)
				.content(content)
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
				chatMessage.getCreatedAt()
		);
	}
}

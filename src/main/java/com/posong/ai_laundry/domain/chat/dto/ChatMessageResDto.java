package com.posong.ai_laundry.domain.chat.dto;

import com.posong.ai_laundry.domain.chat.constant.MessageSenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ChatMessageResDto(
		@Schema(description = "채팅 메시지 ID", example = "1")
		Long chatMessageId,

		@Schema(description = "메시지 발신자 유형", example = "USER")
		MessageSenderType senderType,

		@Schema(description = "채팅 내용", example = "이 니트는 어떻게 세탁해야 해?")
		String content,

		@Schema(description = "메시지 생성 시각", example = "2026-05-11T10:30:00")
		LocalDateTime createdAt
) {
}

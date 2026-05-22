package com.posong.ai_laundry.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatSendResDto(
		@Schema(description = "저장된 사용자 메시지")
		ChatMessageResDto userMessage,

		@Schema(description = "저장된 AI 응답 메시지")
		ChatMessageResDto assistantMessage
) {
}

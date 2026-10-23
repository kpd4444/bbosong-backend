package com.posong.ai_laundry.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ChatSendResDto(
		@Schema(description = "저장된 사용자 메시지")
		ChatMessageResDto userMessage,

		@Schema(description = "저장된 AI 응답 메시지")
		ChatMessageResDto assistantMessage,

		@Schema(description = "세탁 상담 범위 밖 질문일 때 프론트가 띄울 추천 질문 목록입니다. 범위 안이면 빈 목록입니다.", example = "[\"김칫국물이 튀었어요\", \"니트 세탁법\", \"흰 옷이 누렇게 변했어요\"]")
		List<String> suggestedQuestions
) {
}

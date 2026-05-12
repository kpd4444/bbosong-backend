package com.posong.ai_laundry.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageReqDto(
		@Schema(description = "사용자가 전송할 채팅 메시지", example = "이 니트는 어떻게 세탁해야 해?")
		@NotBlank(message = "채팅 내용은 필수입니다.")
		@Size(max = 2000, message = "채팅 내용은 2000자 이하로 입력해주세요.")
		String content
) {
}

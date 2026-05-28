package com.posong.ai_laundry.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberNicknameUpdateReqDto(
		@Schema(description = "변경할 닉네임", example = "bbosong")
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
		String nickname
) {
}

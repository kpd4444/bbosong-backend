package com.posong.ai_laundry.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalLoginReqDto(
		@Schema(description = "로그인 아이디", example = "bbosong_user")
		@NotBlank(message = "로그인 아이디는 필수입니다.")
		@Size(max = 100, message = "로그인 아이디는 100자 이하여야 합니다.")
		String loginId,

		@Schema(description = "비밀번호", example = "password1234")
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
		String password
) {
}

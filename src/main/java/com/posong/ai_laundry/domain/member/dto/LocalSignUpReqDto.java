package com.posong.ai_laundry.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalSignUpReqDto(
		@Schema(description = "로그인 아이디", example = "bbosong_user")
		@NotBlank(message = "로그인 아이디는 필수입니다.")
		@Size(max = 100, message = "로그인 아이디는 100자 이하여야 합니다.")
		String loginId,

		@Schema(description = "비밀번호", example = "password1234")
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
		String password,

		@Schema(description = "이메일", example = "bbosong@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "올바른 이메일 형식이 아닙니다.")
		@Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
		String email
) {
}

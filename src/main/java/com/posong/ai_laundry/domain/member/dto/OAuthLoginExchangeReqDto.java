package com.posong.ai_laundry.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginExchangeReqDto(
		@NotBlank(message = "OAuth login code is required.")
		String code
) {
}

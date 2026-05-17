package com.posong.ai_laundry.domain.member.controller;

import com.posong.ai_laundry.domain.member.dto.OAuthLoginExchangeReqDto;
import com.posong.ai_laundry.domain.member.dto.OAuthLoginExchangeResDto;
import com.posong.ai_laundry.global.response.ApiResponse;
import com.posong.ai_laundry.global.security.OAuth2LoginCodeService;
import com.posong.ai_laundry.global.security.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "OAuth Auth", description = "OAuth login exchange API")
@RequiredArgsConstructor
@RequestMapping("/api/auth/oauth")
public class OAuthAuthController {

	private final OAuth2LoginCodeService oAuth2LoginCodeService;

	@Operation(summary = "Exchange OAuth login code", description = "Exchanges a one-time OAuth login code for tokens.")
	@PostMapping("/exchange")
	public ApiResponse<OAuthLoginExchangeResDto> exchange(@Valid @RequestBody OAuthLoginExchangeReqDto request) {
		return ApiResponse.success(toResponse(oAuth2LoginCodeService.consumeTokenPair(request.code())));
	}

	private OAuthLoginExchangeResDto toResponse(TokenPair tokenPair) {
		return new OAuthLoginExchangeResDto(
				tokenPair.grantType(),
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt(),
				tokenPair.refreshToken(),
				tokenPair.refreshTokenExpiresAt()
		);
	}
}

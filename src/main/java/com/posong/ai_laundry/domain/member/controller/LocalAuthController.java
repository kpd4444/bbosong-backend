package com.posong.ai_laundry.domain.member.controller;

import com.posong.ai_laundry.domain.member.dto.LocalLoginIdCheckResDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueReqDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import com.posong.ai_laundry.domain.member.service.LocalAuthService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Local Auth", description = "Local signup and login API")
@RequiredArgsConstructor
@RequestMapping("/api")
public class LocalAuthController {

	private final LocalAuthService localAuthService;

	@Operation(summary = "Local signup", description = "Registers a member for local login.")
	@PostMapping("/auth/signup/local")
	public ApiResponse<LocalSignUpResDto> signUp(@Valid @RequestBody LocalSignUpReqDto request) {
		return ApiResponse.success(localAuthService.signUp(request));
	}

	@Operation(summary = "Check local login id", description = "Checks whether a local login id is already in use.")
	@GetMapping("/auth/signup/local/check-login-id")
	public ApiResponse<LocalLoginIdCheckResDto> checkLoginId(@RequestParam String loginId) {
		return ApiResponse.success(localAuthService.checkLoginId(loginId));
	}

	@Operation(summary = "Local login", description = "Issues access and refresh tokens after local login.")
	@PostMapping("/auth/login/local")
	public ApiResponse<LocalLoginResDto> login(@Valid @RequestBody LocalLoginReqDto request) {
		return ApiResponse.success(localAuthService.login(request));
	}

	@Operation(summary = "Reissue token", description = "Reissues access and refresh tokens with a refresh token.")
	@PostMapping("/auth/reissue")
	public ApiResponse<TokenReissueResDto> reissue(@Valid @RequestBody TokenReissueReqDto request) {
		return ApiResponse.success(localAuthService.reissue(request));
	}

	@Operation(summary = "Logout", description = "Deletes the current member's refresh token.")
	@PostMapping("/auth/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal Long memberId) {
		localAuthService.logout(memberId);
		return ApiResponse.success();
	}

	@Operation(summary = "My profile", description = "Loads the authenticated member's profile.")
	@GetMapping("/members/me")
	public ApiResponse<MemberProfileResDto> getMyProfile(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(localAuthService.getMyProfile(memberId));
	}

	@Operation(summary = "Withdraw member", description = "Deletes the authenticated member and related data.")
	@DeleteMapping("/members/me")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long memberId) {
		localAuthService.withdraw(memberId);
		return ApiResponse.success();
	}
}

package com.posong.ai_laundry.domain.member.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Local Auth", description = "로컬 회원가입 및 로그인 API")
@RequiredArgsConstructor
@RequestMapping("/api")
public class LocalAuthController {

	private final LocalAuthService localAuthService;

	@Operation(summary = "로컬 회원가입", description = "일반 로그인에 사용할 회원 정보를 등록합니다.")
	@PostMapping("/auth/signup/local")
	public ApiResponse<LocalSignUpResDto> signUp(@Valid @RequestBody LocalSignUpReqDto request) {
		// 로컬 회원가입을 처리한다.
		return ApiResponse.success(localAuthService.signUp(request));
	}

	@Operation(summary = "로컬 로그인", description = "로그인 성공 시 access token과 refresh token을 발급합니다.")
	@PostMapping("/auth/login/local")
	public ApiResponse<LocalLoginResDto> login(@Valid @RequestBody LocalLoginReqDto request) {
		// 로그인 성공 시 access, refresh 토큰을 내려준다.
		return ApiResponse.success(localAuthService.login(request));
	}

	@Operation(summary = "토큰 재발급", description = "refresh token으로 access token과 refresh token을 다시 발급합니다.")
	@PostMapping("/auth/reissue")
	public ApiResponse<TokenReissueResDto> reissue(@Valid @RequestBody TokenReissueReqDto request) {
		// refresh token으로 토큰을 다시 발급한다.
		return ApiResponse.success(localAuthService.reissue(request));
	}

	@Operation(summary = "로그아웃", description = "현재 회원의 refresh token을 제거합니다.")
	@PostMapping("/auth/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal Long memberId) {
		// 현재 회원의 refresh token을 제거한다.
		localAuthService.logout(memberId);
		return ApiResponse.success();
	}

	@Operation(summary = "내 정보 조회", description = "인증된 회원의 기본 정보를 조회합니다.")
	@GetMapping("/members/me")
	public ApiResponse<MemberProfileResDto> getMyProfile(@AuthenticationPrincipal Long memberId) {
		// 인증된 회원의 기본 정보를 조회한다.
		return ApiResponse.success(localAuthService.getMyProfile(memberId));
	}
}

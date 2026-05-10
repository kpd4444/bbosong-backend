package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.dto.LocalLoginReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueReqDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LocalAuthServiceTest {

	@Autowired
	private LocalAuthService localAuthService;

	@Test
	void signUpLoginReissueAndLoadMyProfile() {
		LocalSignUpResDto signUpResponse = localAuthService.signUp(
				new LocalSignUpReqDto(
						"bbosong-user",
						"password1234",
						"bbosong@example.com"
				)
		);

		assertThat(signUpResponse.loginId()).isEqualTo("bbosong-user");
		assertThat(signUpResponse.email()).isEqualTo("bbosong@example.com");

		LocalLoginResDto loginResponse = localAuthService.login(
				new LocalLoginReqDto("bbosong-user", "password1234")
		);

		assertThat(loginResponse.grantType()).isEqualTo("Bearer");
		assertThat(loginResponse.accessToken()).isNotBlank();
		assertThat(loginResponse.refreshToken()).isNotBlank();

		TokenReissueResDto reissueResponse = localAuthService.reissue(
				new TokenReissueReqDto(loginResponse.refreshToken())
		);

		assertThat(reissueResponse.accessToken()).isNotBlank();
		assertThat(reissueResponse.refreshToken()).isNotBlank();

		MemberProfileResDto profileResponse = localAuthService.getMyProfile(signUpResponse.memberId());

		assertThat(profileResponse.email()).isEqualTo("bbosong@example.com");
		assertThat(profileResponse.nickname()).isNull();
		assertThat(profileResponse.birth()).isNull();
	}
}

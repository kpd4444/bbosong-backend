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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LocalAuthServiceTest {

	@Autowired
	private LocalAuthService localAuthService;

	@Test
	void signUpLoginReissueAndLoadMyProfile() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		String loginId = "bbosong-user-" + uniqueSuffix;
		String email = "bbosong-" + uniqueSuffix + "@example.com";

		LocalSignUpResDto signUpResponse = localAuthService.signUp(
				new LocalSignUpReqDto(
						loginId,
						"password1234",
						email
				)
		);

		assertThat(signUpResponse.loginId()).isEqualTo(loginId);
		assertThat(signUpResponse.email()).isEqualTo(email);

		LocalLoginResDto loginResponse = localAuthService.login(
				new LocalLoginReqDto(loginId, "password1234")
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

		assertThat(profileResponse.email()).isEqualTo(email);
		assertThat(profileResponse.nickname()).isNull();
		assertThat(profileResponse.birth()).isNull();
	}
}

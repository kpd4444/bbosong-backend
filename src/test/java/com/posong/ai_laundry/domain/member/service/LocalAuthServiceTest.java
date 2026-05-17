package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.dto.LocalLoginReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueReqDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.LocalAccountRepository;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.security.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class LocalAuthServiceTest {

	@Autowired
	private LocalAuthService localAuthService;

	@Autowired
	private SocialAuthService socialAuthService;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

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
		assertThat(profileResponse.nickname()).isEqualTo(loginId);
		assertThat(profileResponse.birth()).isNull();
	}

	@Test
	void socialLoginCreatesSocialOnlyMemberAndIssuesTokens() {
		String providerUserId = "google-user-" + UUID.randomUUID().toString().substring(0, 8);
		OAuth2MemberInfo memberInfo = new OAuth2MemberInfo(
				SocialProvider.GOOGLE,
				providerUserId,
				providerUserId + "@example.com",
				"Google User"
		);
		String socialNickname = "google_" + providerUserId;

		Member firstLoginMember = socialAuthService.findOrCreateMember(memberInfo);
		Member secondLoginMember = socialAuthService.findOrCreateMember(memberInfo);
		TokenPair tokenPair = authTokenService.issueTokenPair(secondLoginMember);

		assertThat(secondLoginMember.getMemberId()).isEqualTo(firstLoginMember.getMemberId());
		assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, providerUserId))
				.isPresent();
		assertThat(localAccountRepository.findByLoginId(socialNickname)).isEmpty();
		assertThat(tokenPair.grantType()).isEqualTo("Bearer");
		assertThat(tokenPair.accessToken()).isNotBlank();
		assertThat(tokenPair.refreshToken()).isNotBlank();

		assertThatThrownBy(() -> localAuthService.login(
				new LocalLoginReqDto(socialNickname, "password1234")
		)).isInstanceOfSatisfying(GeneralException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.INVALID_LOGIN));
	}
}

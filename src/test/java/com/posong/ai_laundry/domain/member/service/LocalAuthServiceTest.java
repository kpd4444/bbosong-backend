package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.dto.LocalLoginReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginIdCheckResDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberBirthUpdateReqDto;
import com.posong.ai_laundry.domain.member.dto.MemberNicknameUpdateReqDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueReqDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.LocalAccountRepository;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.RefreshTokenRepository;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.security.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
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
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

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

	@Test
	void checkLoginIdReturnsAvailability() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		String loginId = "bbosong-check-" + uniqueSuffix;

		LocalLoginIdCheckResDto availableResponse = localAuthService.checkLoginId(loginId);

		assertThat(availableResponse.loginId()).isEqualTo(loginId);
		assertThat(availableResponse.available()).isTrue();
		assertThat(availableResponse.duplicated()).isFalse();

		localAuthService.signUp(
				new LocalSignUpReqDto(
						loginId,
						"password1234",
						"bbosong-check-" + uniqueSuffix + "@example.com"
				)
		);

		LocalLoginIdCheckResDto duplicatedResponse = localAuthService.checkLoginId(loginId);

		assertThat(duplicatedResponse.loginId()).isEqualTo(loginId);
		assertThat(duplicatedResponse.available()).isFalse();
		assertThat(duplicatedResponse.duplicated()).isTrue();
	}

	@Test
	void updateNicknameChangesMyProfile() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		LocalSignUpResDto signUpResponse = localAuthService.signUp(
				new LocalSignUpReqDto(
						"bbosong-nickname-" + uniqueSuffix,
						"password1234",
						"bbosong-nickname-" + uniqueSuffix + "@example.com"
				)
		);
		String newNickname = "new-nickname-" + uniqueSuffix;

		MemberProfileResDto response = localAuthService.updateNickname(
				signUpResponse.memberId(),
				new MemberNicknameUpdateReqDto(newNickname)
		);

		assertThat(response.nickname()).isEqualTo(newNickname);
		assertThat(localAuthService.getMyProfile(signUpResponse.memberId()).nickname()).isEqualTo(newNickname);
	}

	@Test
	void updateNicknameRejectsDuplicateNickname() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		LocalSignUpResDto firstMember = localAuthService.signUp(
				new LocalSignUpReqDto(
						"bbosong-first-" + uniqueSuffix,
						"password1234",
						"bbosong-first-" + uniqueSuffix + "@example.com"
				)
		);
		LocalSignUpResDto secondMember = localAuthService.signUp(
				new LocalSignUpReqDto(
						"bbosong-second-" + uniqueSuffix,
						"password1234",
						"bbosong-second-" + uniqueSuffix + "@example.com"
				)
		);

		assertThatThrownBy(() -> localAuthService.updateNickname(
				secondMember.memberId(),
				new MemberNicknameUpdateReqDto(firstMember.loginId())
		)).isInstanceOfSatisfying(GeneralException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME));
	}

	@Test
	void updateBirthChangesMyProfile() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		LocalSignUpResDto signUpResponse = localAuthService.signUp(
				new LocalSignUpReqDto(
						"bbosong-birth-" + uniqueSuffix,
						"password1234",
						"bbosong-birth-" + uniqueSuffix + "@example.com"
				)
		);
		LocalDate birth = LocalDate.of(2001, 5, 20);

		MemberProfileResDto response = localAuthService.updateBirth(
				signUpResponse.memberId(),
				new MemberBirthUpdateReqDto(birth)
		);

		assertThat(response.birth()).isEqualTo(birth);
		assertThat(localAuthService.getMyProfile(signUpResponse.memberId()).birth()).isEqualTo(birth);
	}

	@Test
	void withdrawDeletesLocalMemberAndTokens() {
		String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
		String loginId = "bbosong-withdraw-" + uniqueSuffix;
		String email = "bbosong-withdraw-" + uniqueSuffix + "@example.com";

		LocalSignUpResDto signUpResponse = localAuthService.signUp(
				new LocalSignUpReqDto(
						loginId,
						"password1234",
						email
				)
		);
		localAuthService.login(new LocalLoginReqDto(loginId, "password1234"));

		assertThat(memberRepository.findById(signUpResponse.memberId())).isPresent();
		assertThat(localAccountRepository.findByLoginId(loginId)).isPresent();
		assertThat(refreshTokenRepository.findByMember_MemberId(signUpResponse.memberId())).isPresent();

		localAuthService.withdraw(signUpResponse.memberId());

		assertThat(memberRepository.findById(signUpResponse.memberId())).isEmpty();
		assertThat(localAccountRepository.findByLoginId(loginId)).isEmpty();
		assertThat(refreshTokenRepository.findByMember_MemberId(signUpResponse.memberId())).isEmpty();
		assertThatThrownBy(() -> localAuthService.login(new LocalLoginReqDto(loginId, "password1234")))
				.isInstanceOfSatisfying(GeneralException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.INVALID_LOGIN));
	}
}

package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.SocialAccount;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

	@Mock
	private SocialAccountRepository socialAccountRepository;

	@Mock
	private SocialMemberCreationService socialMemberCreationService;

	@InjectMocks
	private SocialAuthService socialAuthService;

	@Test
	void findOrCreateMemberRecoversFromConcurrentSocialAccountCreation() {
		OAuth2MemberInfo memberInfo = new OAuth2MemberInfo(
				SocialProvider.GOOGLE,
				"google-user-123",
				"google-user-123@example.com",
				"Google User"
		);
		Member existingMember = Member.builder()
				.email(memberInfo.email())
				.nickname(SocialAuthService.toNickname(memberInfo))
				.build();
		SocialAccount socialAccount = SocialAccount.builder()
				.member(existingMember)
				.provider(memberInfo.provider())
				.providerUserId(memberInfo.providerUserId())
				.providerEmail(memberInfo.email())
				.build();

		when(socialAccountRepository.findByProviderAndProviderUserId(memberInfo.provider(), memberInfo.providerUserId()))
				.thenReturn(Optional.empty(), Optional.of(socialAccount));
		when(socialMemberCreationService.createSocialMember(memberInfo, SocialAuthService.toNickname(memberInfo)))
				.thenThrow(new DataIntegrityViolationException("duplicate social account"));

		Member result = socialAuthService.findOrCreateMember(memberInfo);

		assertThat(result).isSameAs(existingMember);
		verify(socialMemberCreationService).createSocialMember(memberInfo, SocialAuthService.toNickname(memberInfo));
		verify(socialAccountRepository, times(2))
				.findByProviderAndProviderUserId(memberInfo.provider(), memberInfo.providerUserId());
	}
}

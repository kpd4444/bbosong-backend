package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.SocialAccount;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthService {

	private static final int MAX_NICKNAME_LENGTH = 50;

	private final SocialAccountRepository socialAccountRepository;
	private final SocialMemberCreationService socialMemberCreationService;

	@Transactional
	public Member findOrCreateMember(OAuth2MemberInfo memberInfo) {
		return socialAccountRepository
				.findByProviderAndProviderUserId(memberInfo.provider(), memberInfo.providerUserId())
				.map(SocialAccount::getMember)
				.orElseGet(() -> createSocialMemberSafely(memberInfo));
	}

	private Member createSocialMemberSafely(OAuth2MemberInfo memberInfo) {
		try {
			return socialMemberCreationService.createSocialMember(memberInfo, toNickname(memberInfo));
		} catch (DataIntegrityViolationException e) {
			return socialAccountRepository
					.findByProviderAndProviderUserId(memberInfo.provider(), memberInfo.providerUserId())
					.map(SocialAccount::getMember)
					.orElseThrow(() -> e);
		}
	}

	static String toNickname(OAuth2MemberInfo memberInfo) {
		String nickname = memberInfo.provider().name().toLowerCase() + "_" + memberInfo.providerUserId();
		if (nickname.length() <= MAX_NICKNAME_LENGTH) {
			return nickname;
		}
		return nickname.substring(0, MAX_NICKNAME_LENGTH);
	}
}

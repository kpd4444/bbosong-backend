package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.SocialAccount;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthService {

	private static final int MAX_NICKNAME_LENGTH = 50;

	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Transactional
	public Member findOrCreateMember(OAuth2MemberInfo memberInfo) {
		return socialAccountRepository
				.findByProviderAndProviderUserId(memberInfo.provider(), memberInfo.providerUserId())
				.map(SocialAccount::getMember)
				.orElseGet(() -> createSocialMember(memberInfo));
	}

	private Member createSocialMember(OAuth2MemberInfo memberInfo) {
		Member member = memberRepository.save(
				Member.builder()
						.email(memberInfo.email())
						.nickname(toNickname(memberInfo))
						.build()
		);

		socialAccountRepository.save(
				SocialAccount.builder()
						.member(member)
						.provider(memberInfo.provider())
						.providerUserId(memberInfo.providerUserId())
						.providerEmail(memberInfo.email())
						.build()
		);

		return member;
	}

	private String toNickname(OAuth2MemberInfo memberInfo) {
		String nickname = memberInfo.provider().name().toLowerCase() + "_" + memberInfo.providerUserId();
		if (nickname.length() <= MAX_NICKNAME_LENGTH) {
			return nickname;
		}
		return nickname.substring(0, MAX_NICKNAME_LENGTH);
	}
}

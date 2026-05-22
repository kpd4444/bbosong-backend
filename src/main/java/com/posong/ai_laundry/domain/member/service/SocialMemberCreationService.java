package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.SocialAccount;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SocialMemberCreationService {

	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Member createSocialMember(OAuth2MemberInfo memberInfo, String nickname) {
		Member member = memberRepository.save(
				Member.builder()
						.email(memberInfo.email())
						.nickname(nickname)
						.build()
		);

		socialAccountRepository.saveAndFlush(
				SocialAccount.builder()
						.member(member)
						.provider(memberInfo.provider())
						.providerUserId(memberInfo.providerUserId())
						.providerEmail(memberInfo.email())
						.build()
		);

		return member;
	}
}

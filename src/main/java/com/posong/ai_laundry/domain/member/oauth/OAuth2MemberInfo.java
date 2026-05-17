package com.posong.ai_laundry.domain.member.oauth;

import com.posong.ai_laundry.domain.member.constant.SocialProvider;

public record OAuth2MemberInfo(
		SocialProvider provider,
		String providerUserId,
		String email,
		String nickname
) {
}

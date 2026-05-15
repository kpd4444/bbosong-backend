package com.posong.ai_laundry.domain.member.constant;

import java.util.Locale;

public enum SocialProvider {
	GOOGLE,
	KAKAO;

	public static SocialProvider fromRegistrationId(String registrationId) {
		return SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
	}
}

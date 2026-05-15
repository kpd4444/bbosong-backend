package com.posong.ai_laundry.domain.member.oauth;

import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public final class OAuth2MemberInfoFactory {

	private OAuth2MemberInfoFactory() {
	}

	public static OAuth2MemberInfo from(String registrationId, Map<String, Object> attributes) {
		SocialProvider provider = SocialProvider.fromRegistrationId(registrationId);

		return switch (provider) {
			case GOOGLE -> fromGoogle(attributes);
			case KAKAO -> fromKakao(attributes);
		};
	}

	private static OAuth2MemberInfo fromGoogle(Map<String, Object> attributes) {
		return new OAuth2MemberInfo(
				SocialProvider.GOOGLE,
				requiredString(attributes, "sub"),
				stringValue(attributes.get("email")),
				stringValue(attributes.get("name"))
		);
	}

	@SuppressWarnings("unchecked")
	private static OAuth2MemberInfo fromKakao(Map<String, Object> attributes) {
		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
		Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

		return new OAuth2MemberInfo(
				SocialProvider.KAKAO,
				requiredString(attributes, "id"),
				stringValue(kakaoAccount.get("email")),
				stringValue(profile.get("nickname"))
		);
	}

	private static String requiredString(Map<String, Object> attributes, String key) {
		String value = stringValue(attributes.get(key));
		if (value == null || value.isBlank()) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("invalid_user_info", "Missing OAuth2 user attribute: " + key, null)
			);
		}
		return value;
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}

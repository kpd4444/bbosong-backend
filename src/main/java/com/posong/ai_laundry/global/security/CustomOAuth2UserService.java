package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfo;
import com.posong.ai_laundry.domain.member.oauth.OAuth2MemberInfoFactory;
import com.posong.ai_laundry.domain.member.service.SocialAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final SocialAuthService socialAuthService;
	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = delegate.loadUser(userRequest);
		Member member = socialAuthService.findOrCreateMember(extractMemberInfo(userRequest, oAuth2User));
		return buildAuthenticatedUser(userRequest, oAuth2User, member.getMemberId());
	}

	private OAuth2MemberInfo extractMemberInfo(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		return OAuth2MemberInfoFactory.from(registrationId, oAuth2User.getAttributes());
	}

	private OAuth2User buildAuthenticatedUser(
			OAuth2UserRequest userRequest,
			OAuth2User oAuth2User,
			Long memberId
	) {
		Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
		attributes.put("memberId", memberId);

		String userNameAttributeName = userRequest
				.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUserNameAttributeName();

		return new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				attributes,
				userNameAttributeName
		);
	}
}

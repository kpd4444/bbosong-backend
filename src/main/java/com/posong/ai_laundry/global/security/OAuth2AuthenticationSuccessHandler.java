package com.posong.ai_laundry.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuth2RedirectProperties oAuth2RedirectProperties;
	private final com.posong.ai_laundry.domain.member.service.AuthTokenService authTokenService;

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		Long memberId = oAuth2User.getAttribute("memberId");
		TokenPair tokenPair = authTokenService.issueTokenPair(memberId);

		String redirectUri = UriComponentsBuilder
				.fromUriString(oAuth2RedirectProperties.successRedirectUri())
				.queryParam("grantType", tokenPair.grantType())
				.queryParam("accessToken", tokenPair.accessToken())
				.queryParam("accessTokenExpiresAt", tokenPair.accessTokenExpiresAt())
				.queryParam("refreshToken", tokenPair.refreshToken())
				.queryParam("refreshTokenExpiresAt", tokenPair.refreshTokenExpiresAt())
				.build()
				.encode(StandardCharsets.UTF_8)
				.toUriString();

		response.sendRedirect(redirectUri);
	}

	@ConfigurationProperties(prefix = "app.oauth2")
	public record OAuth2RedirectProperties(String successRedirectUri) {
	}
}

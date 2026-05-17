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
	private final OAuth2LoginCodeService oAuth2LoginCodeService;

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		Long memberId = extractMemberId(authentication);
		String code = issueLoginCode(memberId);

		invalidateSessionIfPresent(request);
		response.sendRedirect(buildRedirectUri(code));
	}

	private Long extractMemberId(Authentication authentication) {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		return oAuth2User.getAttribute("memberId");
	}

	private String issueLoginCode(Long memberId) {
		TokenPair tokenPair = authTokenService.issueTokenPair(memberId);
		return oAuth2LoginCodeService.issueCode(tokenPair);
	}

	private String buildRedirectUri(String code) {
		return UriComponentsBuilder
				.fromUriString(oAuth2RedirectProperties.successRedirectUri())
				.queryParam("code", code)
				.build()
				.encode(StandardCharsets.UTF_8)
				.toUriString();
	}

	private void invalidateSessionIfPresent(HttpServletRequest request) {
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
	}

	@ConfigurationProperties(prefix = "app.oauth2")
	public record OAuth2RedirectProperties(String successRedirectUri) {
	}
}

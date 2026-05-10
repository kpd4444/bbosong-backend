package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

		if (!StringUtils.hasText(bearerToken)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (!bearerToken.startsWith(BEARER_PREFIX)) {
			request.setAttribute(
					JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
					AuthErrorCode.INVALID_ACCESS_TOKEN
			);
			filterChain.doFilter(request, response);
			return;
		}

		String accessToken = bearerToken.substring(BEARER_PREFIX.length());

		if (jwtTokenProvider.isExpired(accessToken)) {
			request.setAttribute(
					JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
					AuthErrorCode.EXPIRED_ACCESS_TOKEN
			);
			filterChain.doFilter(request, response);
			return;
		}

		if (!jwtTokenProvider.isAccessToken(accessToken)) {
			request.setAttribute(
					JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
					AuthErrorCode.INVALID_ACCESS_TOKEN_TYPE
			);
			filterChain.doFilter(request, response);
			return;
		}

		try {
			// access token이 유효하면 인증 정보를 컨텍스트에 넣는다.
			SecurityContextHolder.getContext().setAuthentication(jwtTokenProvider.getAuthentication(accessToken));
		} catch (IllegalArgumentException exception) {
			request.setAttribute(
					JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
					AuthErrorCode.INVALID_ACCESS_TOKEN
			);
		}

		filterChain.doFilter(request, response);
	}
}

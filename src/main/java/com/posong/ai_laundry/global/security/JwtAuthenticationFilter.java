package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import io.jsonwebtoken.JwtException;
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

		// "Bearer " 떼기
		String accessToken = bearerToken.substring(BEARER_PREFIX.length());

		try {
			if (!jwtTokenProvider.isAccessToken(accessToken)) {
				request.setAttribute(
						JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
						AuthErrorCode.INVALID_ACCESS_TOKEN_TYPE
				);
				filterChain.doFilter(request, response);
				return;
			}

			SecurityContextHolder.getContext().setAuthentication(jwtTokenProvider.getAuthentication(accessToken));
		} catch (JwtException exception) {
			request.setAttribute(
					JwtAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTRIBUTE,
					jwtTokenProvider.isExpired(accessToken)
							? AuthErrorCode.EXPIRED_ACCESS_TOKEN
							: AuthErrorCode.INVALID_ACCESS_TOKEN
			);
		}

		filterChain.doFilter(request, response);
	}
}

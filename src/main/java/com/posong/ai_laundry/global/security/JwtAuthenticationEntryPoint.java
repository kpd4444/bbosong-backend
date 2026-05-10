package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	public static final String AUTH_ERROR_CODE_ATTRIBUTE = "authErrorCode";

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException
	) throws IOException {
		Object attribute = request.getAttribute(AUTH_ERROR_CODE_ATTRIBUTE);
		AuthErrorCode errorCode = attribute instanceof AuthErrorCode authErrorCode
				? authErrorCode
				: AuthErrorCode.ACCESS_TOKEN_REQUIRED;

		response.setStatus(errorCode.getStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("""
				{"isSuccess":false,"code":"%s","message":"%s","result":null}
				""".formatted(errorCode.getCode(), errorCode.getMessage()));
	}
}

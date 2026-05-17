package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuth2LoginCodeService {

	private static final Duration CODE_TTL = Duration.ofMinutes(1);

	private final Map<String, PendingLoginCode> pendingCodes = new ConcurrentHashMap<>();

	public String issueCode(TokenPair tokenPair) {
		cleanupExpiredCodes();

		String code = UUID.randomUUID().toString();
		pendingCodes.put(code, new PendingLoginCode(tokenPair, Instant.now().plus(CODE_TTL)));
		return code;
	}

	public TokenPair consumeTokenPair(String code) {
		cleanupExpiredCodes();

		PendingLoginCode pendingLoginCode = pendingCodes.remove(code);
		if (pendingLoginCode == null || pendingLoginCode.expiresAt().isBefore(Instant.now())) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return pendingLoginCode.tokenPair();
	}

	private void cleanupExpiredCodes() {
		Instant now = Instant.now();
		pendingCodes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
	}

	private record PendingLoginCode(TokenPair tokenPair, Instant expiresAt) {
	}
}

package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.domain.member.constant.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	private static final String GRANT_TYPE = "Bearer";

	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	@PostConstruct
	void init() {
		secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public TokenPair generateTokenPair(Long memberId) {
		LocalDateTime accessExpiresAt = LocalDateTime.now().plusSeconds(jwtProperties.accessTokenExpirationSeconds());
		LocalDateTime refreshExpiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

		// 로그인 성공 시 access, refresh 토큰을 함께 발급한다.
		return new TokenPair(
				GRANT_TYPE,
				generateToken(memberId, TokenType.ACCESS, accessExpiresAt),
				accessExpiresAt,
				generateToken(memberId, TokenType.REFRESH, refreshExpiresAt),
				refreshExpiresAt
		);
	}

	public Authentication getAuthentication(String accessToken) {
		Long memberId = getMemberId(accessToken)
				.orElseThrow(() -> new IllegalArgumentException("유효한 액세스 토큰이 아닙니다."));

		return new UsernamePasswordAuthenticationToken(
				memberId,
				null,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}

	public boolean isExpired(String token) {
		try {
			Date expiration = parseClaims(token).getExpiration();
			return expiration.before(new Date());
		} catch (ExpiredJwtException exception) {
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public boolean isRefreshToken(String token) {
		return getTokenType(token)
				.map(TokenType.REFRESH.name()::equals)
				.orElse(false);
	}

	public boolean isAccessToken(String token) {
		return getTokenType(token)
				.map(TokenType.ACCESS.name()::equals)
				.orElse(false);
	}

	public Optional<Long> getMemberId(String token) {
		try {
			return Optional.of(Long.parseLong(parseClaims(token).getSubject()));
		} catch (JwtException | IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private String generateToken(Long memberId, TokenType tokenType, LocalDateTime expiresAt) {
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim(TOKEN_TYPE_CLAIM, tokenType.name())
				.issuedAt(new Date())
				.expiration(toDate(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	private Optional<String> getTokenType(String token) {
		try {
			return Optional.ofNullable(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
		} catch (JwtException | IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private Date toDate(LocalDateTime localDateTime) {
		Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(instant);
	}
}

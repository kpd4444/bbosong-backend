package com.posong.ai_laundry.global.security;

import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2LoginCodeServiceTest {

	private final OAuth2LoginCodeService oAuth2LoginCodeService = new OAuth2LoginCodeService();

	@Test
	void consumeTokenPairReturnsStoredTokenPairOnlyOnce() {
		TokenPair tokenPair = new TokenPair(
				"Bearer",
				"access-token",
				LocalDateTime.now().plusMinutes(30),
				"refresh-token",
				LocalDateTime.now().plusDays(14)
		);
		String code = oAuth2LoginCodeService.issueCode(tokenPair);

		TokenPair consumed = oAuth2LoginCodeService.consumeTokenPair(code);

		assertThat(consumed).isEqualTo(tokenPair);
		assertThatThrownBy(() -> oAuth2LoginCodeService.consumeTokenPair(code))
				.isInstanceOf(GeneralException.class);
	}
}

package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.RefreshToken;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.RefreshTokenRepository;
import com.posong.ai_laundry.global.security.JwtTokenProvider;
import com.posong.ai_laundry.global.security.RefreshTokenHashProvider;
import com.posong.ai_laundry.global.security.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenService {

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenHashProvider refreshTokenHashProvider;

	@Transactional
	public TokenPair issueTokenPair(Member member) {
		TokenPair tokenPair = jwtTokenProvider.generateTokenPair(member.getMemberId());
		saveRefreshToken(member, tokenPair);
		return tokenPair;
	}

	public TokenPair generateTokenPair(Long memberId) {
		return jwtTokenProvider.generateTokenPair(memberId);
	}

	@Transactional
	public TokenPair issueTokenPair(Long memberId) {
		Member member = memberRepository.getReferenceById(memberId);
		return issueTokenPair(member);
	}

	private void saveRefreshToken(Member member, TokenPair tokenPair) {
		String refreshTokenHash = refreshTokenHashProvider.hash(tokenPair.refreshToken());

		refreshTokenRepository.findByMember_MemberId(member.getMemberId())
				.ifPresentOrElse(
						refreshToken -> refreshToken.updateToken(refreshTokenHash, tokenPair.refreshTokenExpiresAt()),
						() -> refreshTokenRepository.save(
								RefreshToken.builder()
										.member(member)
										.token(refreshTokenHash)
										.expiresAt(tokenPair.refreshTokenExpiresAt())
										.build()
						)
				);
	}
}

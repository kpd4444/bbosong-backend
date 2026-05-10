package com.posong.ai_laundry.domain.member.service;

import com.posong.ai_laundry.domain.member.dto.LocalLoginReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueReqDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import com.posong.ai_laundry.domain.member.entity.LocalAccount;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.entity.RefreshToken;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.mapper.MemberMapper;
import com.posong.ai_laundry.domain.member.repository.LocalAccountRepository;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.RefreshTokenRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.security.JwtTokenProvider;
import com.posong.ai_laundry.global.security.TokenPair;
import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalAuthService {

	private final MemberRepository memberRepository;
	private final LocalAccountRepository localAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public LocalSignUpResDto signUp(LocalSignUpReqDto request) {
		validateDuplicateMember(request);

		Member member = memberRepository.save(memberMapper.toMember(request));
		LocalAccount localAccount = localAccountRepository.save(
				memberMapper.toLocalAccount(member, request.loginId(), passwordEncoder.encode(request.password()))
		);

		return memberMapper.toLocalSignUpResDto(member, localAccount);
	}

	@Transactional
	public LocalLoginResDto login(LocalLoginReqDto request) {
		LocalAccount localAccount = localAccountRepository.findByLoginId(request.loginId())
				.orElseThrow(() -> new GeneralException(MemberErrorCode.INVALID_LOGIN));

		// 저장된 비밀번호와 입력값을 비교한다.
		if (!passwordEncoder.matches(request.password(), localAccount.getPassword())) {
			throw new GeneralException(MemberErrorCode.INVALID_LOGIN);
		}

		TokenPair tokenPair = jwtTokenProvider.generateTokenPair(localAccount.getMember().getMemberId());
		saveRefreshToken(localAccount.getMember(), tokenPair.refreshToken(), tokenPair.refreshTokenExpiresAt());

		return memberMapper.toLocalLoginResDto(tokenPair);
	}

	@Transactional
	public TokenReissueResDto reissue(TokenReissueReqDto request) {
		// 만료되었거나 타입이 다른 토큰이면 재발급하지 않는다.
		if (jwtTokenProvider.isExpired(request.refreshToken())) {
			throw new GeneralException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
		}

		if (!jwtTokenProvider.isRefreshToken(request.refreshToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN_TYPE);
		}

		Long memberId = jwtTokenProvider.getMemberId(request.refreshToken());
		RefreshToken refreshToken = refreshTokenRepository.findByMember_MemberId(memberId)
				.orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		if (!refreshToken.getToken().equals(request.refreshToken())) {
			throw new GeneralException(AuthErrorCode.TOKEN_MEMBER_MISMATCH);
		}

		// 재발급 시 access, refresh 토큰을 모두 새로 만든다.
		TokenPair tokenPair = jwtTokenProvider.generateTokenPair(memberId);
		refreshToken.updateToken(tokenPair.refreshToken(), tokenPair.refreshTokenExpiresAt());

		return memberMapper.toTokenReissueResDto(tokenPair);
	}

	@Transactional
	public void logout(Long memberId) {
		refreshTokenRepository.deleteByMember_MemberId(memberId);
	}

	public MemberProfileResDto getMyProfile(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));

		return memberMapper.toMemberProfileResDto(member);
	}

	private void validateDuplicateMember(LocalSignUpReqDto request) {
		if (localAccountRepository.existsByLoginId(request.loginId())) {
			throw new GeneralException(MemberErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (memberRepository.existsByEmail(request.email())) {
			throw new GeneralException(MemberErrorCode.DUPLICATE_EMAIL);
		}
	}

	private void saveRefreshToken(Member member, String token, LocalDateTime expiresAt) {
		// 한 회원당 refresh token 하나만 유지한다.
		refreshTokenRepository.findByMember_MemberId(member.getMemberId())
				.ifPresentOrElse(
						refreshToken -> refreshToken.updateToken(token, expiresAt),
						() -> refreshTokenRepository.save(
								RefreshToken.builder()
										.member(member)
										.token(token)
										.expiresAt(expiresAt)
										.build()
						)
				);
	}
}

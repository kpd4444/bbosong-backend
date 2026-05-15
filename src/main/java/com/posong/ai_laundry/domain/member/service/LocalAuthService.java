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
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.mapper.MemberMapper;
import com.posong.ai_laundry.domain.member.repository.LocalAccountRepository;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.member.repository.RefreshTokenRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.security.JwtTokenProvider;
import com.posong.ai_laundry.global.security.RefreshTokenHashProvider;
import com.posong.ai_laundry.global.security.TokenPair;
import com.posong.ai_laundry.global.security.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final RefreshTokenHashProvider refreshTokenHashProvider;
	private final AuthTokenService authTokenService;

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

		if (!passwordEncoder.matches(request.password(), localAccount.getPassword())) {
			throw new GeneralException(MemberErrorCode.INVALID_LOGIN);
		}

		TokenPair tokenPair = authTokenService.issueTokenPair(localAccount.getMember());

		return memberMapper.toLocalLoginResDto(tokenPair);
	}

	@Transactional
	public TokenReissueResDto reissue(TokenReissueReqDto request) {
		if (jwtTokenProvider.isExpired(request.refreshToken())) {
			throw new GeneralException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
		}

		if (!jwtTokenProvider.isRefreshToken(request.refreshToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN_TYPE);
		}

		Long memberId = jwtTokenProvider.getMemberId(request.refreshToken())
				.orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		String currentRefreshTokenHash = refreshTokenHashProvider.hash(request.refreshToken());

		TokenPair tokenPair = authTokenService.generateTokenPair(memberId);
		int updatedCount = refreshTokenRepository.rotateToken(
				memberId,
				currentRefreshTokenHash,
				refreshTokenHashProvider.hash(tokenPair.refreshToken()),
				tokenPair.refreshTokenExpiresAt()
		);

		if (updatedCount == 0) {
			throw new GeneralException(AuthErrorCode.TOKEN_MEMBER_MISMATCH);
		}

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
		if (memberRepository.existsByNickname(request.loginId())) {
			throw new GeneralException(MemberErrorCode.DUPLICATE_NICKNAME);
		}
	}
}

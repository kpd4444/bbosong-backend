package com.posong.ai_laundry.domain.member.mapper;

import com.posong.ai_laundry.domain.member.dto.LocalLoginResDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpReqDto;
import com.posong.ai_laundry.domain.member.dto.LocalSignUpResDto;
import com.posong.ai_laundry.domain.member.dto.MemberProfileResDto;
import com.posong.ai_laundry.domain.member.dto.TokenReissueResDto;
import com.posong.ai_laundry.domain.member.entity.LocalAccount;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.global.security.TokenPair;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

	private static final int MAX_NICKNAME_LENGTH = 50;

	// 회원가입 요청값으로 Member 엔티티를 만든다.
	public Member toMember(LocalSignUpReqDto request) {
		return Member.builder()
				.email(request.email())
				.nickname(toNickname(request.loginId()))
				.build();
	}

	public LocalAccount toLocalAccount(Member member, String loginId, String encodedPassword) {
		return LocalAccount.builder()
				.member(member)
				.loginId(loginId)
				.password(encodedPassword)
				.build();
	}

	public LocalSignUpResDto toLocalSignUpResDto(Member member, LocalAccount localAccount) {
		return new LocalSignUpResDto(
				member.getMemberId(),
				localAccount.getLoginId(),
				member.getEmail(),
				member.getCreatedAt()
		);
	}

	public LocalLoginResDto toLocalLoginResDto(TokenPair tokenPair) {
		return new LocalLoginResDto(
				tokenPair.grantType(),
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt(),
				tokenPair.refreshToken(),
				tokenPair.refreshTokenExpiresAt()
		);
	}

	public TokenReissueResDto toTokenReissueResDto(TokenPair tokenPair) {
		return new TokenReissueResDto(
				tokenPair.grantType(),
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt(),
				tokenPair.refreshToken(),
				tokenPair.refreshTokenExpiresAt()
		);
	}

	public MemberProfileResDto toMemberProfileResDto(Member member) {
		return new MemberProfileResDto(
				member.getMemberId(),
				member.getEmail(),
				member.getNickname(),
				member.getBirth()
		);
	}

	private String toNickname(String loginId) {
		if (loginId.length() <= MAX_NICKNAME_LENGTH) {
			return loginId;
		}
		return loginId.substring(0, MAX_NICKNAME_LENGTH);
	}
}

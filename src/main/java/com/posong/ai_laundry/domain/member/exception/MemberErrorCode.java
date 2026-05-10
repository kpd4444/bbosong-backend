package com.posong.ai_laundry.domain.member.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

	DUPLICATE_LOGIN_ID("MEMBER_001", "이미 사용 중인 로그인 아이디입니다.", HttpStatus.CONFLICT),
	DUPLICATE_EMAIL("MEMBER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
	DUPLICATE_NICKNAME("MEMBER_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
	INVALID_LOGIN("MEMBER_004", "로그인 아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	MEMBER_NOT_FOUND("MEMBER_005", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

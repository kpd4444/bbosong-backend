package com.posong.ai_laundry.global.security.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

	ACCESS_TOKEN_REQUIRED("AUTH_001", "액세스 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED),
	INVALID_ACCESS_TOKEN("AUTH_002", "유효하지 않은 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
	EXPIRED_ACCESS_TOKEN("AUTH_003", "만료된 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
	INVALID_ACCESS_TOKEN_TYPE("AUTH_004", "액세스 토큰 타입이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	INVALID_REFRESH_TOKEN("AUTH_005", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
	EXPIRED_REFRESH_TOKEN("AUTH_006", "만료된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
	INVALID_REFRESH_TOKEN_TYPE("AUTH_007", "리프레시 토큰 타입이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	TOKEN_MEMBER_MISMATCH("AUTH_008", "토큰 정보가 회원 정보와 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

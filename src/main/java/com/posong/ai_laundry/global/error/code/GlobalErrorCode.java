package com.posong.ai_laundry.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

	INVALID_INPUT_VALUE("COMMON_400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
	INTERNAL_SERVER_ERROR("COMMON_500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

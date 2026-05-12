package com.posong.ai_laundry.domain.chat.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

	CHAT_INPUT_REQUIRED("CHAT_001", "메시지 또는 이미지는 하나 이상 필요합니다.", HttpStatus.BAD_REQUEST),
	INVALID_IMAGE_TYPE("CHAT_002", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
	CHAT_RESPONSE_FAILED("CHAT_003", "채팅 응답 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

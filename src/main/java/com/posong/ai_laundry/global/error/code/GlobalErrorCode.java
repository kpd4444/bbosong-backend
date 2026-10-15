package com.posong.ai_laundry.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

	INVALID_INPUT_VALUE("COMMON_400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
	IMAGE_DIMENSIONS_EXCEEDED("COMMON_400_IMAGE_DIMENSIONS", "이미지는 가로·세로 각각 8192px 이하, 총 2000만 픽셀 이하여야 합니다.", HttpStatus.BAD_REQUEST),
	FILE_SIZE_EXCEEDED("COMMON_413", "업로드 가능한 파일 크기는 최대 20MB입니다.", HttpStatus.CONTENT_TOO_LARGE),
	INTERNAL_SERVER_ERROR("COMMON_500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

package com.posong.ai_laundry.domain.clothes.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClothesErrorCode implements BaseErrorCode {

	IMAGE_REQUIRED("CLOTHES_001", "의류 이미지는 필수입니다.", HttpStatus.BAD_REQUEST),
	INVALID_IMAGE_TYPE("CLOTHES_002", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
	CLOTHES_ANALYSIS_FAILED("CLOTHES_003", "의류 이미지 분석에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_ANALYSIS_RESULT("CLOTHES_004", "의류 분석 결과를 해석할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	CATEGORY_REQUIRED("CLOTHES_005", "의류 카테고리는 필수입니다.", HttpStatus.BAD_REQUEST),
	INVALID_CATEGORY("CLOTHES_006", "지원하지 않는 의류 카테고리입니다.", HttpStatus.BAD_REQUEST),
	CLOTHES_NOT_FOUND("CLOTHES_007", "의류 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	FAVORITE_CONFLICT("CLOTHES_008", "즐겨찾기 상태 변경 중 충돌이 발생했습니다. 다시 시도해 주세요.", HttpStatus.CONFLICT);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

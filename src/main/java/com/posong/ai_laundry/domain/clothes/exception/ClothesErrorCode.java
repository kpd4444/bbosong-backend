package com.posong.ai_laundry.domain.clothes.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClothesErrorCode implements BaseErrorCode {

	IMAGE_REQUIRED("CLOTHES_001", "Clothes image is required.", HttpStatus.BAD_REQUEST),
	INVALID_IMAGE_TYPE("CLOTHES_002", "Only image files can be uploaded.", HttpStatus.BAD_REQUEST),
	CLOTHES_ANALYSIS_FAILED("CLOTHES_003", "Failed to analyze clothes image.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_ANALYSIS_RESULT("CLOTHES_004", "Failed to parse clothes analysis result.", HttpStatus.INTERNAL_SERVER_ERROR),
	CATEGORY_REQUIRED("CLOTHES_005", "Clothes category is required.", HttpStatus.BAD_REQUEST),
	INVALID_CATEGORY("CLOTHES_006", "Unsupported clothes category.", HttpStatus.BAD_REQUEST),
	CLOTHES_NOT_FOUND("CLOTHES_007", "Clothes not found.", HttpStatus.NOT_FOUND),
	FAVORITE_CONFLICT("CLOTHES_008", "Favorite state update conflict. Please retry.", HttpStatus.CONFLICT),
	CLOTHES_ANALYSIS_JOB_NOT_FOUND("CLOTHES_009", "Clothes analysis job not found.", HttpStatus.NOT_FOUND),
	CLOTHES_ANALYSIS_QUEUE_FULL("CLOTHES_010", "Clothes analysis queue is full. Please retry later.", HttpStatus.TOO_MANY_REQUESTS);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

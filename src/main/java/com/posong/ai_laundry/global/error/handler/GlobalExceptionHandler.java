package com.posong.ai_laundry.global.error.handler;

import com.posong.ai_laundry.global.response.ApiResponse;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역에서 발생한 예외를 공통 API 응답 형식으로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 로직에서 의도적으로 발생시킨 커스텀 예외를 처리한다.
	 */
	@ExceptionHandler(GeneralException.class)
	public ResponseEntity<ApiResponse<Void>> handleBaseException(GeneralException exception) {
		return ResponseEntity
				.status(exception.getErrorCode().getStatus())
				.body(ApiResponse.fail(exception.getErrorCode()));
	}


	/**
	 * @Valid 검증 실패 시 첫 번째 필드 오류 메시지를 내려준다.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
		FieldError fieldError = exception.getBindingResult().getFieldError();
		String message = fieldError == null ? GlobalErrorCode.INVALID_INPUT_VALUE.getMessage() : fieldError.getDefaultMessage();

		return ResponseEntity
				.status(GlobalErrorCode.INVALID_INPUT_VALUE.getStatus())
				.body(new ApiResponse<>(false, GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message, null));
	}

	/**
	 * 처리되지 않은 예외를 최종적으로 받아 내부 서버 오류 응답으로 변환한다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		return ResponseEntity
				.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ApiResponse.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR));
	}
}

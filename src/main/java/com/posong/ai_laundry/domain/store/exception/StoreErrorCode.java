package com.posong.ai_laundry.domain.store.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements BaseErrorCode {

	STORE_FAVORITE_ALREADY_EXISTS("STORE_001", "이미 즐겨찾기한 매장입니다.", HttpStatus.CONFLICT),
	STORE_FAVORITE_NOT_FOUND("STORE_002", "즐겨찾기한 매장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

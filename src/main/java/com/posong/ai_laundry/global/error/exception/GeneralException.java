package com.posong.ai_laundry.global.error.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

	private final BaseErrorCode errorCode;

	public GeneralException(BaseErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}

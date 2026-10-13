package com.posong.ai_laundry.domain.clothes.constant;

import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 분석 작업에 저장하거나 사용자에게 공개할 수 있는 실패 사유만 정의한다. */
@Getter
@RequiredArgsConstructor
public enum ClothesAnalysisFailure {
	ANALYSIS_FAILED(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED),
	INVALID_RESULT(ClothesErrorCode.INVALID_ANALYSIS_RESULT),
	QUEUE_FULL(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_FULL);

	private final ClothesErrorCode errorCode;

	public static ClothesAnalysisFailure fromException(Exception exception) {
		if (exception instanceof GeneralException generalException) {
			for (ClothesAnalysisFailure failure : values()) {
				if (failure.errorCode == generalException.getErrorCode()) {
					return failure;
				}
			}
		}
		return ANALYSIS_FAILED;
	}

	/** 기존 스키마를 유지하며, 과거에 저장된 내부 예외 메시지의 노출도 차단한다. */
	public static ClothesAnalysisFailure fromStoredMessage(String message) {
		for (ClothesAnalysisFailure failure : values()) {
			if (failure.errorCode.getMessage().equals(message)) {
				return failure;
			}
		}
		if ("Clothes analysis queue is full.".equals(message)) {
			return QUEUE_FULL;
		}
		return ANALYSIS_FAILED;
	}
}

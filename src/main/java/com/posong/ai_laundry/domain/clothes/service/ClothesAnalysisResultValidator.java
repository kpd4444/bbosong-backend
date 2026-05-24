package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisAiResDto;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.springframework.stereotype.Component;

@Component
public class ClothesAnalysisResultValidator {

	public void validate(ClothesAnalysisAiResDto result) {
		if (result == null
				|| isBlank(result.categoryName())
				|| isBlank(result.name())
				|| isBlank(result.material())
				|| isBlank(result.color())
				|| isBlank(result.washingMethod())
				|| isBlank(result.caution())) {
			throw new GeneralException(ClothesErrorCode.INVALID_ANALYSIS_RESULT);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

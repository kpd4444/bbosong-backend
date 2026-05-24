package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisAiResDto;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClothesAnalysisResponseHarnessTest {

	private final ClothesAnalysisResultValidator validator = new ClothesAnalysisResultValidator();

	@Test
	void acceptsStableClothesAnalysisSamples() {
		List<ClothesAnalysisAiResDto> samples = List.of(
				new ClothesAnalysisAiResDto(
						"top",
						"white cotton t-shirt",
						"cotton material, estimated from image",
						"white",
						"Wash in cold or lukewarm water with mild detergent. Wash separately or with light colors to reduce color transfer risk.",
						"Avoid tumble drying because cotton can shrink. Dry in shade because direct sunlight can cause discoloration."
				),
				new ClothesAnalysisAiResDto(
						"outer",
						"black padded jacket",
						"synthetic outer shell, filling unknown",
						"black",
						"Check the care label first and use a gentle cycle or dry cleaning. Avoid high temperature washing because the filling is uncertain.",
						"Strong spin cycles and tumble drying can deform the shape. Dry fully in a well ventilated place."
				),
				new ClothesAnalysisAiResDto(
						"bottom",
						"blue jeans",
						"denim cotton, estimated from image",
						"blue",
						"Turn inside out and wash separately in cold water. Use a small amount of mild detergent because denim can bleed color.",
						"Do not wash with light colored clothes because dye transfer can occur. Air dry instead of using a dryer."
				)
		);

		for (ClothesAnalysisAiResDto sample : samples) {
			validator.validate(sample);
			assertThat(sample.washingMethod()).hasSizeGreaterThanOrEqualTo(20);
			assertThat(sample.caution()).hasSizeGreaterThanOrEqualTo(20);
		}
	}

	@Test
	void rejectsMissingRequiredFields() {
		ClothesAnalysisAiResDto invalid = new ClothesAnalysisAiResDto(
				"top",
				"white t-shirt",
				"cotton material, estimated from image",
				"white",
				"",
				"Dry in shade."
		);

		assertThatThrownBy(() -> validator.validate(invalid))
				.isInstanceOfSatisfying(GeneralException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ClothesErrorCode.INVALID_ANALYSIS_RESULT));
	}
}

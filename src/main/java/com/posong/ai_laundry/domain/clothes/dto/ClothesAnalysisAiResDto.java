package com.posong.ai_laundry.domain.clothes.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"categoryName", "name", "material", "color", "washingMethod", "caution"})
public record ClothesAnalysisAiResDto(
		@Schema(description = "AI가 분류한 의류 카테고리", example = "상의")
		String categoryName,

		@Schema(description = "AI가 정리한 의류 이름", example = "화이트 니트")
		String name,

		@Schema(description = "사진 기준으로 추정한 소재 정보", example = "면 60%, 폴리에스터 40%")
		String material,

		@Schema(description = "대표 색상", example = "화이트")
		String color,

		@Schema(description = "권장 세탁 방법", example = "찬물 울코스로 중성세제를 사용해 단독 세탁하세요.")
		String washingMethod,

		@Schema(description = "세탁 시 주의사항", example = "건조기 사용은 피하고, 그늘에서 평평하게 건조하세요.")
		String caution
) {
}

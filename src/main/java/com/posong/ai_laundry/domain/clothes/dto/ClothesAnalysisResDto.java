package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesAnalysisResDto(
		@Schema(description = "AI가 추정한 의류 카테고리입니다.", example = "상의")
		String categoryName,

		@Schema(description = "AI가 정리한 의류 이름입니다.", example = "폴로 코튼 케이블 니트")
		String name,

		@Schema(description = "사진 기준으로 추정한 의류 소재 정보입니다.", example = "피마코튼")
		String material,

		@Schema(description = "의류의 주요 색상입니다.", example = "검정색")
		String color,

	@Schema(description = "권장 세탁 방법입니다.", example = "찬물 또는 미온수로 중성세제를 사용해 세탁하세요.")
	String washingMethod,

	@Schema(description = "이염, 수축, 건조기 사용 여부, 표백제, 다림질 등 세탁 시 주의해야 할 사항입니다.", example = "건조기 사용은 피하고, 그늘에서 평평하게 건조하세요.")
	String caution,

	@Schema(description = "사진 기준으로 추정한 구조화 세탁 규칙입니다. 사진만으로 확정할 수 없는 항목은 null로 반환합니다.")
	ClothesWashRulesDto washRules
) {
	public static ClothesAnalysisResDto from(ClothesAnalysisAiResDto source) {
		return new ClothesAnalysisResDto(
				source.categoryName(),
				source.name(),
				source.material(),
				source.color(),
				source.washingMethod(),
				source.caution(),
				source.washRules()
		);
	}
}

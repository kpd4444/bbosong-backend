package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesAnalysisResDto(
		@Schema(description = "AI가 추정한 의류 카테고리입니다. 상의, 하의, 아우터, 원피스, 치마, 속옷, 잠옷, 기타 중 하나로 반환합니다.", example = "상의")
		String categoryName,

		@Schema(description = "사용자가 옷을 구분하기 쉽도록 AI가 정리한 의류 이름입니다.", example = "화이트 니트")
		String name,

		@Schema(description = "사진을 기준으로 추정한 의류 소재 정보입니다. 혼용 소재인 경우 자연어 형태로 반환합니다.", example = "면 60%, 폴리에스터 40%")
		String material,

		@Schema(description = "의류의 대표 색상입니다.", example = "화이트")
		String color,

		@Schema(description = "세탁 온도, 세탁 코스, 세제 사용 방식 등을 포함한 권장 세탁 방법입니다.", example = "찬물 울코스로 중성세제를 사용해 단독 세탁하세요.")
		String washingMethod,

		@Schema(description = "이염, 수축, 건조기 사용 여부, 표백제, 다림질 등 세탁 시 주의해야 할 사항입니다.", example = "건조기 사용은 피하고, 그늘에서 평평하게 건조하세요.")
		String caution
) {
	public static ClothesAnalysisResDto from(ClothesAnalysisAiResDto source) {
		return new ClothesAnalysisResDto(
				source.categoryName(),
				source.name(),
				source.material(),
				source.color(),
				source.washingMethod(),
				source.caution()
		);
	}
}

package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ClothesDetailResDto(
		@Schema(description = "의류 ID", example = "1")
		Long clothesId,

		@Schema(description = "카테고리명", example = "상의")
		String categoryName,

		@Schema(description = "의류 이름", example = "화이트 니트")
		String name,

		@Schema(description = "소재 정보", example = "면 60%, 폴리에스터 40%")
		String material,

		@Schema(description = "대표 색상", example = "화이트")
		String color,

		@Schema(description = "권장 세탁 방법", example = "찬물 울코스로 중성세제를 사용해 단독 세탁하세요.")
		String washingMethod,

		@Schema(description = "세탁 시 주의사항", example = "건조기 사용은 피하고, 그늘에서 평평하게 건조하세요.")
		String caution,

		@Schema(description = "의류 이미지 URL", example = "https://example.com/clothes/white-knit.jpg")
		String imageUrl,

		@Schema(description = "즐겨찾기 여부", example = "false")
		boolean isFavorite,

		@Schema(description = "저장 시각", example = "2026-05-10T16:00:00")
		LocalDateTime createdAt
) {
}

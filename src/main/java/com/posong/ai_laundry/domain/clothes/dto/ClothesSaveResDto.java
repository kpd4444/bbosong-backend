package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ClothesSaveResDto(
		@Schema(description = "저장된 의류 ID", example = "1")
		Long clothesId,

		@Schema(description = "정규화된 카테고리명", example = "상의")
		String categoryName,

		@Schema(description = "저장된 의류 이름", example = "화이트 니트")
		String name,

		@Schema(description = "저장 시각", example = "2026-05-10T16:00:00")
		LocalDateTime createdAt
) {
}

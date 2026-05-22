package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesFavoriteResDto(
		@Schema(description = "의류 ID", example = "1")
		Long clothesId,

		@Schema(description = "변경 후 즐겨찾기 여부", example = "true")
		boolean isFavorite
) {
}

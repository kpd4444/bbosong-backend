package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesFavoriteReqDto(
		@Schema(description = "설정할 즐겨찾기 상태", example = "true")
		boolean favorite
) {
}

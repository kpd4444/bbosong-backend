package com.posong.ai_laundry.domain.clothes.dto;

import java.util.List;

public record ClothesHomeResDto(
		List<ClothesSummaryResDto> recentClothes,
		List<ClothesSummaryResDto> favoriteClothes
) {
}

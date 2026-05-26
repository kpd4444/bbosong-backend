package com.posong.ai_laundry.domain.clothes.mapper;

import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesDetailResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesFavoriteResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSummaryResDto;
import com.posong.ai_laundry.domain.clothes.entity.Category;
import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClothesMapper {

	private final ImageUrlResolver imageUrlResolver;

	public Clothes toClothes(Member member, Category category, ClothesSaveReqDto request, String imageKey) {
		return Clothes.builder()
				.member(member)
				.category(category)
				.name(request.name())
				.material(request.material())
				.color(request.color())
				.washingMethod(request.washingMethod())
				.caution(request.caution())
				.imageKey(imageKey)
				.isFavorite(false)
				.build();
	}

	public ClothesSaveResDto toClothesSaveResDto(Clothes clothes) {
		return new ClothesSaveResDto(
				clothes.getClothesId(),
				clothes.getCategory().getName(),
				clothes.getName(),
				clothes.getCreatedAt()
		);
	}

	public ClothesSummaryResDto toClothesSummaryResDto(Clothes clothes) {
		return new ClothesSummaryResDto(
				clothes.getClothesId(),
				clothes.getCategory() == null ? null : clothes.getCategory().getName(),
				clothes.getName(),
				clothes.getColor(),
				imageUrlResolver.resolve(clothes.getImageKey()),
				clothes.isFavorite(),
				clothes.getCreatedAt()
		);
	}

	public ClothesDetailResDto toClothesDetailResDto(Clothes clothes) {
		return new ClothesDetailResDto(
				clothes.getClothesId(),
				clothes.getCategory() == null ? null : clothes.getCategory().getName(),
				clothes.getName(),
				clothes.getMaterial(),
				clothes.getColor(),
				clothes.getWashingMethod(),
				clothes.getCaution(),
				imageUrlResolver.resolve(clothes.getImageKey()),
				clothes.isFavorite(),
				clothes.getCreatedAt()
		);
	}

	public ClothesFavoriteResDto toClothesFavoriteResDto(Clothes clothes) {
		return new ClothesFavoriteResDto(
				clothes.getClothesId(),
				clothes.isFavorite()
		);
	}
}

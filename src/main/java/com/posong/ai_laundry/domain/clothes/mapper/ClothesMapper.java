package com.posong.ai_laundry.domain.clothes.mapper;

import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSummaryResDto;
import com.posong.ai_laundry.domain.clothes.entity.Category;
import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import com.posong.ai_laundry.domain.member.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class ClothesMapper {

	public Clothes toClothes(Member member, Category category, ClothesSaveReqDto request) {
		return Clothes.builder()
				.member(member)
				.category(category)
				.name(request.name())
				.material(request.material())
				.color(request.color())
				.washingMethod(request.washingMethod())
				.caution(request.caution())
				.imageUrl(request.imageUrl())
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
				clothes.getImageUrl(),
				clothes.isFavorite(),
				clothes.getCreatedAt()
		);
	}
}

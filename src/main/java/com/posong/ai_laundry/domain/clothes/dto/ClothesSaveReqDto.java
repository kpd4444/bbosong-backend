package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClothesSaveReqDto(
		@Schema(description = "의류 카테고리입니다. 분석 API 결과의 categoryName 값을 그대로 보내면 됩니다.", example = "상의")
		@NotBlank(message = "의류 카테고리는 필수입니다.")
		@Size(max = 50, message = "의류 카테고리는 50자 이하여야 합니다.")
		String categoryName,

		@Schema(description = "저장할 의류 이름입니다.", example = "화이트 니트")
		@NotBlank(message = "의류 이름은 필수입니다.")
		@Size(max = 100, message = "의류 이름은 100자 이하여야 합니다.")
		String name,

		@Schema(description = "의류 소재 정보입니다.", example = "면 60%, 폴리에스터 40%")
		@NotBlank(message = "의류 소재는 필수입니다.")
		@Size(max = 255, message = "의류 소재는 255자 이하여야 합니다.")
		String material,

		@Schema(description = "대표 색상입니다.", example = "화이트")
		@NotBlank(message = "의류 색상은 필수입니다.")
		@Size(max = 100, message = "의류 색상은 100자 이하여야 합니다.")
		String color,

		@Schema(description = "권장 세탁 방법입니다.", example = "찬물 울코스로 중성세제를 사용해 단독 세탁하세요.")
		@NotBlank(message = "세탁 방법은 필수입니다.")
		@Size(max = 255, message = "세탁 방법은 255자 이하여야 합니다.")
		String washingMethod,

		@Schema(description = "세탁 시 주의사항입니다.", example = "건조기 사용은 피하고, 그늘에서 평평하게 건조하세요.")
		@NotBlank(message = "주의사항은 필수입니다.")
		@Size(max = 500, message = "주의사항은 500자 이하여야 합니다.")
		String caution,

		@Schema(description = "의류 이미지 URL입니다. 없으면 null로 보낼 수 있습니다.", example = "https://example.com/clothes/white-knit.jpg")
		@Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
		String imageUrl
) {
}

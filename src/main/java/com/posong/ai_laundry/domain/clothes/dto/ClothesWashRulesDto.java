package com.posong.ai_laundry.domain.clothes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesWashRulesDto(
		@Schema(description = "물세탁 가능 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "true")
		Boolean waterWash,

		@Schema(description = "권장 최대 세탁 온도입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "30")
		Integer maxWaterTemperature,

		@Schema(description = "표백제 사용 가능 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "false")
		Boolean bleachAllowed,

		@Schema(description = "건조기 사용 가능 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "false")
		Boolean dryerAllowed,

		@Schema(description = "다림질 가능 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "true")
		Boolean ironAllowed,

		@Schema(description = "드라이클리닝 가능 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "false")
		Boolean dryCleanAllowed,

		@Schema(description = "손세탁 필요 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "false")
		Boolean handWashRequired,

		@Schema(description = "단독 세탁 필요 여부입니다. 사진만으로 확정할 수 없으면 null입니다.", example = "true")
		Boolean separateWashRequired
) {
}

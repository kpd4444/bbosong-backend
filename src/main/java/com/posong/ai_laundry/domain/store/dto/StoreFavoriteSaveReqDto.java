package com.posong.ai_laundry.domain.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StoreFavoriteSaveReqDto(
		@Schema(description = "카카오 장소 ID", example = "123456789")
		@NotBlank(message = "카카오 장소 ID는 필수입니다.")
		String kakaoPlaceId,

		@Schema(description = "매장 이름", example = "크린토피아 코인워시 인천연수서해그랑블점")
		@NotBlank(message = "매장 이름은 필수입니다.")
		String name,

		@Schema(description = "매장 주소", example = "인천 연수구 송도동 123-45")
		String address,

		@Schema(description = "매장 전화번호", example = "032-123-4567")
		String phone,

		@Schema(description = "위도", example = "37.1234567")
		@NotNull(message = "위도는 필수입니다.")
		BigDecimal latitude,

		@Schema(description = "경도", example = "127.1234567")
		@NotNull(message = "경도는 필수입니다.")
		BigDecimal longitude,

		@Schema(description = "카카오 장소 URL", example = "https://place.map.kakao.com/123456789")
		String placeUrl
) {
}

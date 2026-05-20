package com.posong.ai_laundry.domain.weather.controller;

import com.posong.ai_laundry.domain.weather.dto.WeatherLaundryResDto;
import com.posong.ai_laundry.domain.weather.service.WeatherLaundryService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Validated
@Tag(name = "Weather", description = "날씨 기반 세탁 추천 API")
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

	private final WeatherLaundryService weatherLaundryService;

	@Operation(
			summary = "날씨 기반 세탁 추천 조회",
			description = "사용자 위치의 기상청 단기예보를 기반으로 세탁 추천 카드를 조회합니다."
	)
	@GetMapping("/laundry")
	public ApiResponse<WeatherLaundryResDto> getLaundryWeather(
			@Parameter(description = "위도", example = "37.5665")
			@DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
			@DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
			@RequestParam BigDecimal latitude,
			@Parameter(description = "경도", example = "126.9780")
			@DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
			@DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
			@RequestParam BigDecimal longitude
	) {
		return ApiResponse.success(weatherLaundryService.getLaundryWeather(latitude, longitude));
	}
}

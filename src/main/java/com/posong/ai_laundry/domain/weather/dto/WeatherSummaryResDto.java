package com.posong.ai_laundry.domain.weather.dto;

public record WeatherSummaryResDto(
		Integer temperature,
		Integer humidity,
		Integer rainProbability,
		String skyStatus,
		String precipitationType
) {
}

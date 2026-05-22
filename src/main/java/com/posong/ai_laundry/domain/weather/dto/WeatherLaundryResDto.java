package com.posong.ai_laundry.domain.weather.dto;

import java.util.List;

public record WeatherLaundryResDto(
		WeatherSummaryResDto weatherSummary,
		List<LaundryRecommendationResDto> recommendations
) {
}

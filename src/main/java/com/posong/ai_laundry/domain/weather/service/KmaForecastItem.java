package com.posong.ai_laundry.domain.weather.service;

public record KmaForecastItem(
		String baseDate,
		String baseTime,
		String forecastDate,
		String forecastTime,
		String category,
		String forecastValue
) {
}

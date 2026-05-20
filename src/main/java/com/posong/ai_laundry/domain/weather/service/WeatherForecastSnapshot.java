package com.posong.ai_laundry.domain.weather.service;

public record WeatherForecastSnapshot(
		String baseDate,
		String baseTime,
		String forecastDate,
		String forecastTime,
		Integer temperature,
		Integer humidity,
		Integer rainProbability,
		String skyStatus,
		String precipitationType
) {
}

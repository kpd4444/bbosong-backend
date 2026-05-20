package com.posong.ai_laundry.domain.weather.service;

import java.time.LocalDate;

public record WeatherBaseTime(
		LocalDate baseDate,
		String baseTime
) {
}

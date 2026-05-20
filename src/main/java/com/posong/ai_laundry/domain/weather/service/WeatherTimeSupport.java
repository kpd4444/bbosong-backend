package com.posong.ai_laundry.domain.weather.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class WeatherTimeSupport {

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final List<Integer> BASE_HOURS = List.of(2, 5, 8, 11, 14, 17, 20, 23);

	private WeatherTimeSupport() {
	}

	public static WeatherBaseTime latestBaseTime() {
		LocalDateTime targetTime = LocalDateTime.now(SEOUL_ZONE).minusMinutes(10);
		LocalDate baseDate = targetTime.toLocalDate();

		for (int index = BASE_HOURS.size() - 1; index >= 0; index--) {
			int baseHour = BASE_HOURS.get(index);
			if (targetTime.getHour() >= baseHour) {
				return new WeatherBaseTime(baseDate, "%02d00".formatted(baseHour));
			}
		}

		return new WeatherBaseTime(baseDate.minusDays(1), "2300");
	}
}

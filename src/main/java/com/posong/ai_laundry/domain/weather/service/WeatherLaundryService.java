package com.posong.ai_laundry.domain.weather.service;

import com.posong.ai_laundry.domain.weather.dto.LaundryRecommendationResDto;
import com.posong.ai_laundry.domain.weather.dto.WeatherLaundryResDto;
import com.posong.ai_laundry.domain.weather.dto.WeatherSummaryResDto;
import com.posong.ai_laundry.domain.weather.exception.WeatherErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherLaundryService {

	private final KmaForecastClient kmaForecastClient;
	private final KmaGridConverter kmaGridConverter;
	private final WeatherRecommendationService weatherRecommendationService;

	public WeatherLaundryResDto getLaundryWeather(BigDecimal latitude, BigDecimal longitude) {
		KmaGridCoordinate coordinate = kmaGridConverter.convert(latitude, longitude);
		WeatherBaseTime baseTime = WeatherTimeSupport.latestBaseTime();
		List<KmaForecastItem> forecastItems = kmaForecastClient.getVillageForecast(
				baseTime.baseDate(),
				baseTime.baseTime(),
				coordinate.nx(),
				coordinate.ny()
		);
		WeatherForecastSnapshot snapshot = selectForecastSnapshot(forecastItems);
		List<LaundryRecommendationResDto> recommendations = weatherRecommendationService.recommend(snapshot);

		return new WeatherLaundryResDto(
				new WeatherSummaryResDto(
						snapshot.temperature(),
						snapshot.humidity(),
						snapshot.rainProbability(),
						snapshot.skyStatus(),
						snapshot.precipitationType()
				),
				recommendations
		);
	}

	private WeatherForecastSnapshot selectForecastSnapshot(List<KmaForecastItem> forecastItems) {
		Map<String, Map<String, KmaForecastItem>> groupedItems = new HashMap<>();
		for (KmaForecastItem item : forecastItems) {
			if (isLaundryWeatherCategory(item.category())) {
				String forecastKey = item.forecastDate() + item.forecastTime();
				groupedItems.computeIfAbsent(forecastKey, key -> new HashMap<>())
						.put(item.category(), item);
			}
		}

		return groupedItems.entrySet().stream()
				.filter(entry -> containsRequiredCategories(entry.getValue()))
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.map(entry -> toSnapshot(entry.getValue()))
				.findFirst()
				.orElseThrow(() -> new GeneralException(WeatherErrorCode.WEATHER_FORECAST_NOT_FOUND));
	}

	private boolean isLaundryWeatherCategory(String category) {
		return "TMP".equals(category)
				|| "REH".equals(category)
				|| "POP".equals(category)
				|| "SKY".equals(category)
				|| "PTY".equals(category);
	}

	private boolean containsRequiredCategories(Map<String, KmaForecastItem> items) {
		return items.containsKey("TMP")
				&& items.containsKey("REH")
				&& items.containsKey("POP")
				&& items.containsKey("SKY")
				&& items.containsKey("PTY");
	}

	private WeatherForecastSnapshot toSnapshot(Map<String, KmaForecastItem> items) {
		KmaForecastItem temperature = items.get("TMP");
		return new WeatherForecastSnapshot(
				temperature.baseDate(),
				temperature.baseTime(),
				temperature.forecastDate(),
				temperature.forecastTime(),
				toInteger(temperature.forecastValue()),
				toInteger(items.get("REH").forecastValue()),
				toInteger(items.get("POP").forecastValue()),
				toSkyStatus(items.get("SKY").forecastValue()),
				toPrecipitationType(items.get("PTY").forecastValue())
		);
	}

	private Integer toInteger(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private String toSkyStatus(String value) {
		return switch (value) {
			case "1" -> "맑음";
			case "3" -> "구름많음";
			case "4" -> "흐림";
			default -> "알 수 없음";
		};
	}

	private String toPrecipitationType(String value) {
		return switch (value) {
			case "0" -> "없음";
			case "1" -> "비";
			case "2" -> "비/눈";
			case "3" -> "눈";
			case "4" -> "소나기";
			default -> "알 수 없음";
		};
	}
}

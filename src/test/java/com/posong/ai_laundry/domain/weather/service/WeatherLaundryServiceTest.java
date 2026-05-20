package com.posong.ai_laundry.domain.weather.service;

import com.posong.ai_laundry.domain.weather.dto.WeatherLaundryResDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherLaundryServiceTest {

	@Mock
	private KmaForecastClient kmaForecastClient;

	@Spy
	private KmaGridConverter kmaGridConverter = new KmaGridConverter();

	@Spy
	private WeatherRecommendationService weatherRecommendationService = new WeatherRecommendationService();

	@InjectMocks
	private WeatherLaundryService weatherLaundryService;

	@Test
	void getLaundryWeatherRecommendsIndoorDryWhenRainy() {
		when(kmaForecastClient.getVillageForecast(any(), anyString(), anyInt(), anyInt()))
				.thenReturn(List.of(
						item("TMP", "22"),
						item("REH", "85"),
						item("POP", "70"),
						item("SKY", "4"),
						item("PTY", "1")
				));

		WeatherLaundryResDto result = weatherLaundryService.getLaundryWeather(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780)
		);

		assertThat(result.weatherSummary().humidity()).isEqualTo(85);
		assertThat(result.weatherSummary().rainProbability()).isEqualTo(70);
		assertThat(result.weatherSummary().precipitationType()).isEqualTo("비");
		assertThat(result.recommendations()).extracting("title")
				.containsExactly("실내건조 추천", "두꺼운 빨래는 미루기");
	}

	@Test
	void getLaundryWeatherRecommendsOutdoorDryWhenClearAndDry() {
		when(kmaForecastClient.getVillageForecast(any(), anyString(), anyInt(), anyInt()))
				.thenReturn(List.of(
						item("TMP", "25"),
						item("REH", "45"),
						item("POP", "10"),
						item("SKY", "1"),
						item("PTY", "0")
				));

		WeatherLaundryResDto result = weatherLaundryService.getLaundryWeather(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780)
		);

		assertThat(result.weatherSummary().skyStatus()).isEqualTo("맑음");
		assertThat(result.weatherSummary().precipitationType()).isEqualTo("없음");
		assertThat(result.recommendations()).extracting("title")
				.containsExactly("실외건조 추천", "두꺼운 빨래 가능");
	}

	@Test
	void convertLatitudeAndLongitudeToKmaGridCoordinate() {
		KmaGridCoordinate coordinate = kmaGridConverter.convert(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780)
		);

		assertThat(coordinate.nx()).isEqualTo(60);
		assertThat(coordinate.ny()).isEqualTo(127);
	}

	private KmaForecastItem item(String category, String value) {
		return new KmaForecastItem(
				"20260520",
				"1100",
				"20260520",
				"1200",
				category,
				value
		);
	}
}

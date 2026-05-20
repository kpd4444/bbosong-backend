package com.posong.ai_laundry.domain.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posong.ai_laundry.domain.weather.config.KmaWeatherProperties;
import com.posong.ai_laundry.domain.weather.exception.WeatherErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KmaForecastClient {

	private static final String SUCCESS_CODE = "00";

	private final KmaWeatherProperties kmaWeatherProperties;
	private final ObjectMapper objectMapper;
	private final RestClient.Builder restClientBuilder;

	public List<KmaForecastItem> getVillageForecast(LocalDate baseDate, String baseTime, int nx, int ny) {
		if (!StringUtils.hasText(kmaWeatherProperties.serviceKey())) {
			throw new GeneralException(WeatherErrorCode.WEATHER_SERVICE_KEY_REQUIRED);
		}

		String uri = UriComponentsBuilder
				.fromUriString(kmaWeatherProperties.endpoint())
				.path("/getVilageFcst")
				.queryParam("serviceKey", kmaWeatherProperties.serviceKey())
				.queryParam("pageNo", 1)
				.queryParam("numOfRows", 1000)
				.queryParam("dataType", "JSON")
				.queryParam("base_date", baseDate.format(WeatherTimeSupport.DATE_FORMATTER))
				.queryParam("base_time", baseTime)
				.queryParam("nx", nx)
				.queryParam("ny", ny)
				.build(false)
				.toUriString();

		try {
			String response = restClientBuilder.build()
					.get()
					.uri(uri)
					.retrieve()
					.body(String.class);

			return parseForecastItems(response);
		} catch (GeneralException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new GeneralException(WeatherErrorCode.WEATHER_API_FAILED);
		}
	}

	private List<KmaForecastItem> parseForecastItems(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		JsonNode responseNode = root.path("response");
		String resultCode = responseNode.path("header").path("resultCode").asText();
		if (!SUCCESS_CODE.equals(resultCode)) {
			throw new GeneralException(WeatherErrorCode.WEATHER_API_FAILED);
		}

		JsonNode items = responseNode.path("body").path("items").path("item");
		if (!items.isArray() || items.isEmpty()) {
			throw new GeneralException(WeatherErrorCode.WEATHER_FORECAST_NOT_FOUND);
		}

		List<KmaForecastItem> result = new ArrayList<>();
		for (JsonNode item : items) {
			result.add(new KmaForecastItem(
					item.path("baseDate").asText(),
					item.path("baseTime").asText(),
					item.path("fcstDate").asText(),
					item.path("fcstTime").asText(),
					item.path("category").asText(),
					item.path("fcstValue").asText()
			));
		}
		return result;
	}
}

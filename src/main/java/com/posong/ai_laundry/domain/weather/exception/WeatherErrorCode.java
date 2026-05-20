package com.posong.ai_laundry.domain.weather.exception;

import com.posong.ai_laundry.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WeatherErrorCode implements BaseErrorCode {

	WEATHER_SERVICE_KEY_REQUIRED("WEATHER_001", "기상청 API 서비스키가 필요합니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	WEATHER_API_FAILED("WEATHER_002", "기상청 날씨 정보를 조회하지 못했습니다.", HttpStatus.BAD_GATEWAY),
	WEATHER_FORECAST_NOT_FOUND("WEATHER_003", "세탁 추천에 필요한 날씨 예보를 찾을 수 없습니다.", HttpStatus.BAD_GATEWAY);

	private final String code;
	private final String message;
	private final HttpStatus status;
}

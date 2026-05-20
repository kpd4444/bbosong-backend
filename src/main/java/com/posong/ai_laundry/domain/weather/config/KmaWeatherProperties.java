package com.posong.ai_laundry.domain.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.kma")
public record KmaWeatherProperties(
		String serviceKey,
		String endpoint
) {
}

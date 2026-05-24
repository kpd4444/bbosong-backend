package com.posong.ai_laundry.global.ai;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiChatOptionsFactory {

	private final String model;
	private final Double temperature;

	public OpenAiChatOptionsFactory(
			@Value("${spring.ai.openai.chat.options.model:gpt-5-mini}") String model,
			@Value("${external-api.openai.temperature:0.2}") Double temperature
	) {
		validateModel(model);
		validateTemperature(temperature);

		this.model = model;
		this.temperature = temperature;
	}

	public OpenAiChatOptions create() {
		OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
				.model(model);

		if (supportsTemperature(model)) {
			builder.temperature(temperature);
		}

		return builder.build();
	}

	private void validateModel(String model) {
		if (!StringUtils.hasText(model)) {
			throw new IllegalArgumentException("spring.ai.openai.chat.options.model must not be blank");
		}
	}

	private void validateTemperature(Double temperature) {
		if (temperature == null || temperature.isNaN() || temperature < 0.0 || temperature > 2.0) {
			throw new IllegalArgumentException("external-api.openai.temperature must be between 0.0 and 2.0");
		}
	}

	private boolean supportsTemperature(String model) {
		return !model.startsWith("gpt-5");
	}
}

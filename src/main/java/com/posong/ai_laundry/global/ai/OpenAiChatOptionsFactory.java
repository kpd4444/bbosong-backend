package com.posong.ai_laundry.global.ai;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiChatOptionsFactory {

	private final String model;
	private final Double temperature;

	public OpenAiChatOptionsFactory(
			@Value("${spring.ai.openai.chat.options.model:gpt-5-mini}") String model,
			@Value("${external-api.openai.temperature:0.2}") Double temperature
	) {
		this.model = model;
		this.temperature = temperature;
	}

	public OpenAiChatOptions create() {
		return OpenAiChatOptions.builder()
				.model(model)
				.temperature(temperature)
				.build();
	}
}

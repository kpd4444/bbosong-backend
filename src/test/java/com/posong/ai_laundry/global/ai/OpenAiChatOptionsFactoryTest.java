package com.posong.ai_laundry.global.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiChatOptionsFactoryTest {

	@Test
	void createsStableOpenAiChatOptions() {
		OpenAiChatOptionsFactory factory = new OpenAiChatOptionsFactory("gpt-5-mini", 0.2);

		OpenAiChatOptions options = factory.create();

		assertThat(options.getModel()).isEqualTo("gpt-5-mini");
		assertThat(options.getTemperature()).isEqualTo(0.2);
	}
}

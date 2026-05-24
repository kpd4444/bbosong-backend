package com.posong.ai_laundry.global.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiChatOptionsFactoryTest {

	@Test
	void createsOptionsWhenPropertiesAreValid() {
		OpenAiChatOptionsFactory factory = new OpenAiChatOptionsFactory("gpt-5-mini", 0.2);

		assertThatCode(factory::create).doesNotThrowAnyException();
	}

	@Test
	void omitsTemperatureForGpt5Models() {
		OpenAiChatOptions options = new OpenAiChatOptionsFactory("gpt-5-mini", 0.2).create();

		assertThat(options.getTemperature()).isNull();
	}

	@Test
	void appliesTemperatureForNonGpt5Models() {
		OpenAiChatOptions options = new OpenAiChatOptionsFactory("gpt-4o-mini", 0.2).create();

		assertThat(options.getModel()).isEqualTo("gpt-4o-mini");
		assertThat(options.getTemperature()).isEqualTo(0.2);
	}

	@Test
	void rejectsBlankModel() {
		assertThatThrownBy(() -> new OpenAiChatOptionsFactory(" ", 0.2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("spring.ai.openai.chat.options.model must not be blank");
	}

	@Test
	void rejectsInvalidTemperature() {
		assertThatThrownBy(() -> new OpenAiChatOptionsFactory("gpt-5-mini", 2.1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("external-api.openai.temperature must be between 0.0 and 2.0");
	}
}

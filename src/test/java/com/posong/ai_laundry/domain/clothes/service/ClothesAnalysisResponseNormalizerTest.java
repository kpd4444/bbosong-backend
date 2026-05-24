package com.posong.ai_laundry.domain.clothes.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClothesAnalysisResponseNormalizerTest {

	@Test
	void removesJsonCodeFence() {
		String response = """
				```json
				{"categoryName":"top","name":"white t-shirt"}
				```
				""";

		assertThat(ClothesAnalysisResponseNormalizer.normalize(response))
				.isEqualTo("{\"categoryName\":\"top\",\"name\":\"white t-shirt\"}");
	}

	@Test
	void extractsJsonObjectFromExplanatoryText() {
		String response = """
				Here is the result:
				{"categoryName":"top","name":"brace } inside string","caution":"escape \\" quote"}
				Use this value.
				""";

		assertThat(ClothesAnalysisResponseNormalizer.normalize(response))
				.isEqualTo("{\"categoryName\":\"top\",\"name\":\"brace } inside string\",\"caution\":\"escape \\\" quote\"}");
	}
}

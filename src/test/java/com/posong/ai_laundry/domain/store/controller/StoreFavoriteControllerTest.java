package com.posong.ai_laundry.domain.store.controller;

import com.posong.ai_laundry.domain.store.service.StoreFavoriteService;
import com.posong.ai_laundry.global.error.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoreFavoriteControllerTest {

	private StoreFavoriteService service;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(StoreFavoriteService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		// Authentication is outside this standalone request-validation test's scope.
		mockMvc = MockMvcBuilders.standaloneSetup(new StoreFavoriteController(service))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"http://place.map.kakao.com/123456789",
			"https://place.map.kakao.com/123456789"
	})
	void acceptsKakaoPlaceUrlsAndOptionalEmptyValues(String placeUrl) throws Exception {
		mockMvc.perform(post("/api/stores/favorites")
				.contentType(MediaType.APPLICATION_JSON).content(requestBody(placeUrl)))
				.andExpect(status().isOk());

		verify(service).saveFavorite(isNull(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://evil.example/123456789",
			"https://place.map.kakao.com.evil.example/123456789",
			"https://place.map.kakao.com@evil.example/123456789",
			"https://evil.example@place.map.kakao.com/123456789",
			"https://place.map.kakao.com:8443/123456789",
			"javascript:alert(1)",
			"//place.map.kakao.com/123456789",
			"https://place.map.kakao.com/",
			"https://place.map.kakao.com/not-a-place-id",
			"https://place.map.kakao.com/123?redirect=https://evil.example",
			"https://place.map.kakao.com/123#https://evil.example",
			"https://place.map.kakao.com/123/../456",
			"https://place%2emap.kakao.com/123",
			" https://place.map.kakao.com/123",
			"https://place.map.kakao.com/123 ",
			" "
	})
	void rejectsInvalidUrlsBeforeCallingService(String placeUrl) throws Exception {
		mockMvc.perform(post("/api/stores/favorites")
				.contentType(MediaType.APPLICATION_JSON).content(requestBody(placeUrl)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("message").value(
						"카카오 장소 URL은 http(s)://place.map.kakao.com/숫자ID 형식이어야 합니다."));

		verifyNoInteractions(service);
	}

	private String requestBody(String placeUrl) {
		String urlValue = placeUrl == null ? "null" : "\"" + placeUrl + "\"";
		return """
				{
				  "kakaoPlaceId": "123456789",
				  "name": "Laundry",
				  "latitude": 37.5,
				  "longitude": 127.0,
				  "placeUrl": %s
				}
				""".formatted(urlValue);
	}
}

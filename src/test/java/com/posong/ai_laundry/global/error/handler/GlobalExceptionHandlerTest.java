package com.posong.ai_laundry.global.error.handler;

import com.posong.ai_laundry.domain.member.controller.LocalAuthController;
import com.posong.ai_laundry.domain.member.service.LocalAuthService;
import com.posong.ai_laundry.domain.store.controller.StoreFavoriteController;
import com.posong.ai_laundry.domain.store.service.StoreFavoriteService;
import com.posong.ai_laundry.domain.clothes.controller.ClothesAnalysisController;
import com.posong.ai_laundry.domain.clothes.service.ClothesAnalysisAsyncService;
import com.posong.ai_laundry.domain.weather.controller.WeatherController;
import com.posong.ai_laundry.domain.weather.service.WeatherLaundryService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

	private LocalAuthService authService;
	private StoreFavoriteService favoriteService;
	private GlobalExceptionHandler handler;
	private ClothesAnalysisAsyncService analysisService;
	private WeatherLaundryService weatherService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = mock(LocalAuthService.class);
		favoriteService = mock(StoreFavoriteService.class);
		analysisService = mock(ClothesAnalysisAsyncService.class);
		weatherService = mock(WeatherLaundryService.class);
		handler = spy(new GlobalExceptionHandler());
		mockMvc = MockMvcBuilders.standaloneSetup(
				new LocalAuthController(authService), new StoreFavoriteController(favoriteService),
				new ClothesAnalysisController(analysisService), new WeatherController(weatherService),
				new InvalidReturnController())
				.setControllerAdvice(handler)
				.build();
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		assertBadRequest(mockMvc.perform(post("/api/auth/login/local")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":")), HttpMessageNotReadableException.class);
	}

	@Test
	void missingRequiredParameterReturnsBadRequest() throws Exception {
		assertBadRequest(mockMvc.perform(get("/api/auth/signup/local/check-login-id")),
				MissingServletRequestParameterException.class);
	}

	@Test
	void invalidPathVariableTypeReturnsBadRequest() throws Exception {
		assertBadRequest(mockMvc.perform(delete("/api/stores/favorites/abc")),
				MethodArgumentTypeMismatchException.class);
	}

	@Test
	void missingImageReturnsBadRequest() throws Exception {
		assertBadRequest(mockMvc.perform(multipart("/api/clothes/analysis")),
				MissingServletRequestPartException.class);
	}

	@ParameterizedTest
	@CsvSource({"-90.1,127", "90.1,127", "37,-180.1", "37,180.1"})
	void invalidCoordinatesReturnBadRequest(String latitude, String longitude) throws Exception {
		assertBadRequest(mockMvc.perform(get("/api/weather/laundry")
				.param("latitude", latitude).param("longitude", longitude)),
				HandlerMethodValidationException.class);
	}

	@ParameterizedTest
	@CsvSource({"-90,-180", "90,180"})
	void coordinateBoundariesReachService(String latitude, String longitude) throws Exception {
		mockMvc.perform(get("/api/weather/laundry")
				.param("latitude", latitude).param("longitude", longitude))
				.andExpect(status().isOk());
		verify(weatherService).getLaundryWeather(new java.math.BigDecimal(latitude), new java.math.BigDecimal(longitude));
	}

	@Test
	void returnValueValidationIsStillServerError() throws Exception {
		mockMvc.perform(get("/test/invalid-return"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("code").value("COMMON_500"));
	}

	@RestController
	static class InvalidReturnController {
		@GetMapping("/test/invalid-return")
		@NotNull
		public String invalidReturn() {
			return null;
		}
	}

	@Test
	void unexpectedExceptionStillReturnsInternalServerError() throws Exception {
		IllegalStateException failure = new IllegalStateException("internal details");
		when(authService.checkLoginId("test-user")).thenThrow(failure);

		mockMvc.perform(get("/api/auth/signup/local/check-login-id").param("loginId", "test-user"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("isSuccess").value(false))
				.andExpect(jsonPath("code").value("COMMON_500"))
				.andExpect(jsonPath("message").value("서버 내부 오류가 발생했습니다."))
				.andExpect(jsonPath("result").doesNotExist());

		verify(handler).handleException(failure);
	}

	private void assertBadRequest(ResultActions response, Class<? extends Exception> exceptionType) throws Exception {
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("isSuccess").value(false))
				.andExpect(jsonPath("code").value("COMMON_400"))
				.andExpect(jsonPath("message").value("잘못된 요청입니다."))
				.andExpect(jsonPath("result").doesNotExist())
				.andExpect(result -> assertInstanceOf(exceptionType, result.getResolvedException()));

		verifyNoInteractions(authService, favoriteService, analysisService, weatherService);
		verify(handler, never()).handleException(any(Exception.class));
	}
}

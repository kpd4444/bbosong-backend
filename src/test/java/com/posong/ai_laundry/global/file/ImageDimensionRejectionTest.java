package com.posong.ai_laundry.global.file;

import com.posong.ai_laundry.domain.chat.service.ChatService;
import com.posong.ai_laundry.domain.clothes.controller.ClothesAnalysisController;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import com.posong.ai_laundry.domain.clothes.repository.ClothesRepository;
import com.posong.ai_laundry.domain.clothes.repository.CategoryRepository;
import com.posong.ai_laundry.domain.clothes.service.ClothesService;
import com.posong.ai_laundry.domain.clothes.service.ClothesAnalysisService;
import com.posong.ai_laundry.domain.clothes.service.ClothesAnalysisAsyncService;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.error.handler.GlobalExceptionHandler;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ImageDimensionRejectionTest {
	@Mock MemberRepository members;
	@Mock ClothesRepository clothes;
	@Mock CategoryRepository categories;
	@Mock ImageStorageService storage;
	@Mock ClothesAnalysisJobRepository jobs;
	@Mock ChatModel model;
	@Mock Executor executor;
	@InjectMocks ClothesService clothesService;
	@InjectMocks ChatService chatService;
	@InjectMocks ClothesAnalysisService analyzer;

	@Test
	void clothesRejectsBeforeStorageOrDatabaseWrites() throws Exception {
		when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		var image = ImageFileValidatorTest.oversizedPng();
		assertThatThrownBy(() -> clothesService.save(1L, null, image))
				.isInstanceOf(ImageDimensionsExceededException.class);
		verifyNoInteractions(storage, clothes, categories);
	}

	@Test
	void chatRejectsBeforeStorageOrAi() throws Exception {
		when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		var image = ImageFileValidatorTest.oversizedPng();
		assertThatThrownBy(() -> chatService.sendMessage(1L, "사진 분석", image))
				.isInstanceOf(ImageDimensionsExceededException.class);
		verifyNoInteractions(storage, model);
	}

	@Test
	void synchronousAnalysisRejectsBeforeAi() throws Exception {
		var image = ImageFileValidatorTest.oversizedPng();
		assertThatThrownBy(() -> analyzer.analyze(image)).isInstanceOf(ImageDimensionsExceededException.class);
		verifyNoInteractions(model);
	}

	@Test
	void analysisRequestReturns400BeforeJobCreationOrExecution() throws Exception {
		ClothesAnalysisAsyncService service = new ClothesAnalysisAsyncService(jobs, analyzer, executor, null);
		var mvc = MockMvcBuilders.standaloneSetup(new ClothesAnalysisController(service))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
		mvc.perform(multipart("/api/clothes/analysis").file(ImageFileValidatorTest.oversizedPng()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("isSuccess").value(false))
				.andExpect(jsonPath("code").value("COMMON_400_IMAGE_DIMENSIONS"));
		verifyNoInteractions(jobs, executor, model);
	}
}

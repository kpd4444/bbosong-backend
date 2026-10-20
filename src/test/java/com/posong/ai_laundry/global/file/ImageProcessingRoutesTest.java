package com.posong.ai_laundry.global.file;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.GpsDirectory;
import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import com.posong.ai_laundry.domain.chat.mapper.ChatMapper;
import com.posong.ai_laundry.domain.chat.repository.ChatMessageRepository;
import com.posong.ai_laundry.domain.chat.repository.ChatRoomRepository;
import com.posong.ai_laundry.domain.chat.service.ChatService;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveReqDto;
import com.posong.ai_laundry.domain.clothes.entity.Category;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import com.posong.ai_laundry.domain.clothes.mapper.ClothesMapper;
import com.posong.ai_laundry.domain.clothes.repository.*;
import com.posong.ai_laundry.domain.clothes.service.*;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.ai.OpenAiChatOptionsFactory;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitBreaker;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageProcessingRoutesTest {
	@Mock MemberRepository members;
	@Mock ClothesRepository clothes;
	@Mock CategoryRepository categories;
	@Mock ClothesMapper clothesMapper;
	@Mock ChatRoomRepository rooms;
	@Mock ChatMessageRepository messages;
	@Mock ChatMapper chatMapper;
	@Mock ImageStorageService storage;
	@Mock ClothesAnalysisJobRepository jobs;
	@Mock Executor executor;
	@Mock ChatModel model;
	@Mock ExternalApiCircuitBreaker circuit;
	@Mock OpenAiChatOptionsFactory options;
	@InjectMocks ClothesService clothesService;
	@InjectMocks ChatService chatService;
	@InjectMocks ClothesAnalysisService analyzer;

	@Test
	void clothesStorageReceivesProcessedImage() throws Exception {
		when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		when(categories.findByName("상의")).thenReturn(Optional.of(mock(Category.class)));
		clothesService.save(1L, new ClothesSaveReqDto("상의", "셔츠", "면", "빨강", "세탁", "주의"),
				ImageFixtures.jpegWithExif(6));
		var capture = ArgumentCaptor.forClass(ProcessedImage.class);
		verify(storage).uploadClothesImage(capture.capture());
		assertSanitized(capture.getValue().bytes());
	}

	@Test
	void chatUsesIdenticalSanitizedBytesForStorageAndAi() throws Exception {
		when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		when(rooms.findByMember_MemberId(1L)).thenReturn(Optional.of(mock(ChatRoom.class)));
		when(messages.save(any())).thenReturn(mock(ChatMessage.class));
		runAiSupplier();
		var original = ImageFixtures.jpegWithExif(6);
		assertThatThrownBy(() -> chatService.sendMessage(1L, "사진 분석", original)).isInstanceOf(GeneralException.class);
		var capture = ArgumentCaptor.forClass(ProcessedImage.class);
		verify(storage).uploadChatImage(capture.capture());
		byte[] sentToAi = capturedMedia();
		assertThat(sentToAi).isEqualTo(capture.getValue().bytes());
		assertSanitized(sentToAi);
	}

	@Test
	void directAnalysisSendsSanitizedImageToAi() throws Exception {
		runAiSupplier();
		var original = ImageFixtures.jpegWithExif(6);
		assertThatThrownBy(() -> analyzer.analyze(original)).isInstanceOf(GeneralException.class);
		assertSanitized(capturedMedia());
	}

	@Test
	void asyncJobPersistsProcessedBytesAndUsesThemForAi() throws Exception {
		when(jobs.saveAndFlush(any())).thenAnswer(invocation -> {
			ClothesAnalysisJob job = invocation.getArgument(0);
			org.springframework.test.util.ReflectionTestUtils.setField(job, "analysisJobId", 1L);
			return job;
		});
		var service = new ClothesAnalysisAsyncService(jobs, analyzer, executor, null);
		service.submit(1L, ImageFixtures.jpegWithExif(6));
		var capture = ArgumentCaptor.forClass(ClothesAnalysisJob.class);
		verify(jobs).saveAndFlush(capture.capture());
		var job = capture.getValue();
		assertSanitized(job.getImageData());
		assertThat(job.getImageContentType()).isEqualTo("image/jpeg");
		runAiSupplier();
		assertThatThrownBy(() -> analyzer.analyze(job.getImageData(), MimeTypeUtils.IMAGE_JPEG))
				.isInstanceOf(GeneralException.class);
		assertThat(capturedMedia()).isEqualTo(job.getImageData());
	}

	@Test
	void processingFailureStopsAllOutboundPaths() throws Exception {
		when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		var invalid = ImageFixtures.jpegWithExif(9);
		assertThatThrownBy(() -> clothesService.save(1L, null, invalid)).isInstanceOf(GeneralException.class);
		assertThatThrownBy(() -> chatService.sendMessage(1L, "분석", invalid)).isInstanceOf(GeneralException.class);
		assertThatThrownBy(() -> analyzer.analyze(invalid)).isInstanceOf(GeneralException.class);
		var service = new ClothesAnalysisAsyncService(jobs, analyzer, executor, null);
		assertThatThrownBy(() -> service.submit(1L, invalid)).isInstanceOf(GeneralException.class);
		verifyNoInteractions(storage, model, circuit, jobs, executor, clothes, categories, rooms, messages);
	}

	private void runAiSupplier() {
		when(circuit.execute(anyString(), any(), any())).thenAnswer(invocation ->
				((Supplier<?>) invocation.getArgument(2)).get());
		// Stop after capturing the real prompt; no network call is performed.
		when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("test upstream failure"));
	}

	private byte[] capturedMedia() {
		var capture = ArgumentCaptor.forClass(Prompt.class);
		verify(model).call(capture.capture());
		UserMessage message = (UserMessage) capture.getValue().getInstructions().getLast();
		assertThat(message.getMedia().getFirst().getMimeType()).isEqualTo(MimeTypeUtils.IMAGE_JPEG);
		return (byte[]) message.getMedia().getFirst().getData();
	}

	private void assertSanitized(byte[] bytes) throws Exception {
		var image = ImageIO.read(new ByteArrayInputStream(bytes));
		assertThat(image.getWidth()).isEqualTo(40);
		assertThat(image.getHeight()).isEqualTo(60);
		assertThat(ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes))
				.getFirstDirectoryOfType(GpsDirectory.class)).isNull();
	}
}

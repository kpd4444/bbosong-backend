package com.posong.ai_laundry.domain.chat.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.posong.ai_laundry.domain.chat.constant.ChatScopeMessages;
import com.posong.ai_laundry.domain.chat.constant.ChatScopeReason;
import com.posong.ai_laundry.domain.chat.dto.ChatSendResDto;
import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import com.posong.ai_laundry.domain.chat.mapper.ChatMapper;
import com.posong.ai_laundry.domain.chat.repository.ChatMessageRepository;
import com.posong.ai_laundry.domain.chat.repository.ChatRoomRepository;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.ai.OpenAiChatOptionsFactory;
import com.posong.ai_laundry.global.file.ImageFixtures;
import com.posong.ai_laundry.global.file.ProcessedImage;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitBreaker;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import com.posong.ai_laundry.global.storage.ImageUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatServiceScopeTest {

	private ChatMessageRepository chatMessageRepository;
	private ChatModel chatModel;
	private ChatScopeClassifier classifier;
	private ImageStorageService imageStorageService;
	private ChatService chatService;
	private final List<ChatMessage> savedMessages = new ArrayList<>();

	@BeforeEach
	void setUp() {
		ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		chatMessageRepository = mock(ChatMessageRepository.class);
		chatModel = mock(ChatModel.class);
		classifier = mock(ChatScopeClassifier.class);
		imageStorageService = mock(ImageStorageService.class);

		ChatRoom chatRoom = mock(ChatRoom.class);
		when(chatRoom.getChatRoomId()).thenReturn(1L);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		when(chatRoomRepository.findByMember_MemberId(1L)).thenReturn(Optional.of(chatRoom));
		when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
			ChatMessage message = invocation.getArgument(0);
			ReflectionTestUtils.setField(message, "chatMessageId", (long) savedMessages.size() + 1);
			savedMessages.add(message);
			return message;
		});
		when(chatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAscChatMessageIdAsc(1L))
				.thenAnswer(invocation -> List.copyOf(savedMessages));
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("세탁 답변")))));

		chatService = new ChatService(
				chatRoomRepository,
				chatMessageRepository,
				memberRepository,
				new ChatMapper(mock(ImageUrlResolver.class)),
				chatModel,
				new ExternalApiCircuitBreaker(3, Duration.ofSeconds(30), 10),
				new OpenAiChatOptionsFactory("gpt-5-mini", 0.2),
				imageStorageService,
				classifier
		);
		ReflectionTestUtils.setField(chatService, "openAiCallTimeout", Duration.ofSeconds(10));
	}

	@Test
	void outOfScopeMessageGetsSavedRejectionWithoutMainCall() {
		when(classifier.classify(anyList(), anyString()))
				.thenReturn(new ChatScopeClassifier.ScopeResult(false, ChatScopeReason.UNRELATED_TOPIC));

		ChatSendResDto response = chatService.sendMessage(1L, "범위 밖 질문", null);

		verify(chatModel, never()).call(any(Prompt.class));
		assertThat(response.assistantMessage().content()).isEqualTo(ChatScopeMessages.REJECTION_MESSAGE);
		assertThat(response.suggestedQuestions()).isEqualTo(ChatScopeMessages.SUGGESTED_QUESTIONS);
		assertThat(savedMessages).hasSize(2);
	}

	@Test
	void inScopeMessageCallsMainModelWithEmptySuggestions() {
		when(classifier.classify(anyList(), anyString()))
				.thenReturn(new ChatScopeClassifier.ScopeResult(true, ChatScopeReason.LAUNDRY_CARE));

		ChatSendResDto response = chatService.sendMessage(1L, "범위 안 질문", null);

		verify(chatModel).call(any(Prompt.class));
		assertThat(response.assistantMessage().content()).isEqualTo("세탁 답변");
		assertThat(response.suggestedQuestions()).isEmpty();
	}

	@Test
	void logsOnlyCategoryForOutOfScopeAndNothingForInScope() {
		Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			when(classifier.classify(anyList(), anyString()))
					.thenReturn(new ChatScopeClassifier.ScopeResult(true, ChatScopeReason.FOLLOW_UP));
			chatService.sendMessage(1L, "범위 안 후속 질문", null);
			assertThat(appender.list).isEmpty();

			when(classifier.classify(anyList(), anyString()))
					.thenReturn(new ChatScopeClassifier.ScopeResult(false, ChatScopeReason.UNRELATED_TOPIC));
			chatService.sendMessage(1L, "개인정보가 담긴 범위 밖 질문", null);
			assertThat(appender.list).singleElement().satisfies(event -> {
				assertThat(event.getFormattedMessage()).isEqualTo("Out-of-scope chat message rejected: reason=UNRELATED_TOPIC");
				assertThat(event.getFormattedMessage()).doesNotContain("개인정보");
			});
		} finally {
			logger.detachAppender(appender);
		}
	}

	@Test
	void classifierExcludesCurrentMessageFromHistory() {
		when(classifier.classify(anyList(), anyString()))
				.thenReturn(new ChatScopeClassifier.ScopeResult(true, ChatScopeReason.LAUNDRY_CARE));

		chatService.sendMessage(1L, "첫 질문", null);

		verify(classifier).classify(List.of(), "첫 질문");
	}

	@Test
	void imageMessageSkipsClassification() throws Exception {
		when(imageStorageService.uploadChatImage(any(ProcessedImage.class))).thenReturn("chat/image.png");

		ChatSendResDto response = chatService.sendMessage(1L, "이 옷 어떻게 빨아?", ImageFixtures.pngWithExif(1));

		verifyNoInteractions(classifier);
		verify(chatModel).call(any(Prompt.class));
		assertThat(response.suggestedQuestions()).isEmpty();
	}
}

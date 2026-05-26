package com.posong.ai_laundry.domain.chat.service;

import com.posong.ai_laundry.domain.chat.constant.MessageSenderType;
import com.posong.ai_laundry.domain.chat.dto.ChatMessageResDto;
import com.posong.ai_laundry.domain.chat.dto.ChatSendResDto;
import com.posong.ai_laundry.domain.chat.entity.ChatMessage;
import com.posong.ai_laundry.domain.chat.entity.ChatRoom;
import com.posong.ai_laundry.domain.chat.exception.ChatErrorCode;
import com.posong.ai_laundry.domain.chat.mapper.ChatMapper;
import com.posong.ai_laundry.domain.chat.repository.ChatMessageRepository;
import com.posong.ai_laundry.domain.chat.repository.ChatRoomRepository;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.ai.OpenAiChatOptionsFactory;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.file.ImageFileSizeExceededException;
import com.posong.ai_laundry.global.file.ImageFileValidator;
import com.posong.ai_laundry.global.resilience.ExternalApiCallTimeoutException;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitBreaker;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitOpenException;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

	private static final String OPENAI_CIRCUIT = "openai";
	private static final int MAX_CONTEXT_MESSAGES = 40;

	private static final String SYSTEM_PROMPT = """
			너는 의류 관리 상담을 돕는 AI다.
			사용자의 질문과 첨부 이미지를 바탕으로 세탁, 건조, 보관, 얼룩 제거, 의류 손상 방지 방법을 안내한다.

			상담 원칙:
			- 항상 한국어로 답한다.
			- 사용자가 바로 실행할 수 있게 단계와 기준을 구체적으로 말한다.
			- 확실하지 않은 내용은 단정하지 말고 "일반적인 기준", "라벨 확인 필요", "사진만으로는 추정"처럼 표현한다.
			- 의류 손상 위험이 있으면 보수적으로 안내한다.
			- 고온 세탁, 건조기, 표백제, 강한 탈수, 다림질은 소재가 확실하지 않으면 권하지 않는다.
			- 세탁 라벨 확인이 중요한 경우 반드시 언급한다.
			- 사용자가 위험한 방법을 물어보면 더 안전한 대안을 제시한다.

			답변 구성:
			- 질문이 간단하면 2~4문장으로 간결하게 답한다.
			- 세탁 방법을 묻는 경우 물 온도, 세탁 코스, 세제, 단독 세탁 여부를 포함한다.
			- 건조 방법을 묻는 경우 건조기 사용 가능성, 그늘 건조, 형태 유지 방법을 포함한다.
			- 얼룩 제거를 묻는 경우 문지르기보다 두드려 제거하고, 눈에 띄지 않는 부분에 먼저 테스트하라고 안내한다.
			- 이미지가 첨부되었지만 판단이 어려우면 추정 가능한 부분과 확인이 필요한 부분을 나누어 말한다.
			- 불필요한 자기소개, 장황한 설명, 마크다운 표는 사용하지 않는다.
			""";

	private static final String IMAGE_ONLY_PLACEHOLDER = "[이미지 첨부]";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberRepository memberRepository;
	private final ChatMapper chatMapper;
	private final ChatModel chatModel;
	private final ExternalApiCircuitBreaker externalApiCircuitBreaker;
	private final OpenAiChatOptionsFactory openAiChatOptionsFactory;
	private final ImageStorageService imageStorageService;

	@Value("${external-api.openai.call-timeout:60s}")
	private Duration openAiCallTimeout;

	public List<ChatMessageResDto> getMessages(Long memberId) {
		validateMember(memberId);
		return chatRoomRepository.findByMember_MemberId(memberId)
				.map(chatRoom -> chatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAscChatMessageIdAsc(chatRoom.getChatRoomId())
						.stream()
						.map(chatMapper::toChatMessageResDto)
						.toList())
				.orElseGet(List::of);
	}

	@Transactional
	public ChatSendResDto sendMessage(Long memberId, String content, MultipartFile image) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
		MimeType imageMimeType = validateInput(content, image);

		ChatRoom chatRoom = chatRoomRepository.findByMember_MemberId(memberId)
				.orElseGet(() -> createChatRoomSafely(member));

		String normalizedContent = normalizeContent(content);
		String savedUserContent = hasText(normalizedContent) ? normalizedContent : IMAGE_ONLY_PLACEHOLDER;
		String imageKey = image == null || image.isEmpty()
				? null
				: imageStorageService.uploadChatImage(image, imageMimeType);

		try {
			ChatMessage userMessage = chatMessageRepository.save(chatMapper.toUserMessage(chatRoom, savedUserContent, imageKey));
			List<ChatMessage> messages = chatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAscChatMessageIdAsc(chatRoom.getChatRoomId());
			if (messages.size() > MAX_CONTEXT_MESSAGES) {
				messages = messages.subList(messages.size() - MAX_CONTEXT_MESSAGES, messages.size());
			}

			String assistantAnswer = generateAssistantAnswer(
					messages,
					userMessage.getChatMessageId(),
					normalizedContent,
					image,
					imageMimeType
			);
			ChatMessage assistantMessage = chatMessageRepository.save(chatMapper.toAssistantMessage(chatRoom, assistantAnswer));

			return new ChatSendResDto(
					chatMapper.toChatMessageResDto(userMessage),
					chatMapper.toChatMessageResDto(assistantMessage)
			);
		} catch (GeneralException exception) {
			imageStorageService.delete(imageKey);
			throw exception;
		} catch (Exception exception) {
			imageStorageService.delete(imageKey);
			log.warn("Failed to generate chat response", exception);
			throw new GeneralException(ChatErrorCode.CHAT_RESPONSE_FAILED);
		}
	}

	@Transactional
	public void deleteMessages(Long memberId) {
		validateMember(memberId);
		chatRoomRepository.findByMember_MemberId(memberId)
				.ifPresent(chatMessageRepository::deleteAllByChatRoom);
	}

	private String generateAssistantAnswer(
			List<ChatMessage> messages,
			Long currentUserMessageId,
			String currentContent,
			MultipartFile image,
			MimeType imageMimeType
	) {
		List<Message> promptMessages = new ArrayList<>();
		promptMessages.add(new SystemMessage(SYSTEM_PROMPT));

		for (ChatMessage message : messages) {
			if (message.getChatMessageId().equals(currentUserMessageId)) {
				continue;
			}
			if (message.getSenderType() == MessageSenderType.USER) {
				promptMessages.add(new UserMessage(message.getContent()));
			} else {
				promptMessages.add(new AssistantMessage(message.getContent()));
			}
		}

		promptMessages.add(buildCurrentUserMessage(currentContent, image, imageMimeType));

		String responseText;
		try {
			responseText = externalApiCircuitBreaker.execute(OPENAI_CIRCUIT, openAiCallTimeout, () ->
					chatModel.call(new Prompt(promptMessages, openAiChatOptionsFactory.create()))
							.getResult()
							.getOutput()
							.getText()
			);
		} catch (ExternalApiCircuitOpenException | ExternalApiCallTimeoutException exception) {
			log.warn("OpenAI chat response failed by circuit breaker or timeout", exception);
			throw new GeneralException(ChatErrorCode.CHAT_RESPONSE_FAILED);
		}

		if (!hasText(responseText)) {
			throw new GeneralException(ChatErrorCode.CHAT_RESPONSE_FAILED);
		}

		return responseText.trim();
	}

	private UserMessage buildCurrentUserMessage(String content, MultipartFile image, MimeType imageMimeType) {
		String promptText = hasText(content)
				? content
				: "첨부한 이미지를 보고 의류 관리 방법을 알려줘.";

		if (image == null || image.isEmpty()) {
			return new UserMessage(promptText);
		}

		return UserMessage.builder()
				.text(promptText)
				.media(new Media(imageMimeType, image.getResource()))
				.build();
	}

	private void validateMember(Long memberId) {
		memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private ChatRoom createChatRoomSafely(Member member) {
		try {
			return chatRoomRepository.save(ChatRoom.builder().member(member).build());
		} catch (DataIntegrityViolationException exception) {
			return chatRoomRepository.findByMember_MemberId(member.getMemberId())
					.orElseThrow(() -> exception);
		}
	}

	private MimeType validateInput(String content, MultipartFile image) {
		if (!hasText(content) && (image == null || image.isEmpty())) {
			throw new GeneralException(ChatErrorCode.CHAT_INPUT_REQUIRED);
		}

		if (image == null || image.isEmpty()) {
			return null;
		}

		try {
			return ImageFileValidator.detectSupportedMimeType(image);
		} catch (ImageFileSizeExceededException exception) {
			throw new GeneralException(GlobalErrorCode.FILE_SIZE_EXCEEDED);
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ChatErrorCode.INVALID_IMAGE_TYPE);
		}
	}

	private String normalizeContent(String content) {
		return content == null ? null : content.trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

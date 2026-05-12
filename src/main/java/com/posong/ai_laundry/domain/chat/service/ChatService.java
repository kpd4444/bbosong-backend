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
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

	private static final String SYSTEM_PROMPT = """
			너는 세탁 상담을 도와주는 AI다.
			사용자의 질문에 한국어로 답하고 세탁 방법, 건조 방법, 주의사항을 실용적으로 설명한다.
			모르는 내용은 단정하지 말고 일반적인 기준임을 분명히 말한다.
			위험하거나 확신할 수 없는 내용은 보수적으로 안내한다.
			답변은 지나치게 길지 않게 하되 실제로 바로 따라할 수 있게 구체적으로 설명한다.
			""";

	private static final String IMAGE_ONLY_PLACEHOLDER = "[이미지 첨부]";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberRepository memberRepository;
	private final ChatMapper chatMapper;
	private final ChatModel chatModel;

	@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
	private String openAiModel;

	public List<ChatMessageResDto> getMessages(Long memberId) {
		validateMember(memberId);
		return chatRoomRepository.findByMember_MemberId(memberId)
				.map(chatRoom -> chatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(chatRoom.getChatRoomId())
						.stream()
						.map(chatMapper::toChatMessageResDto)
						.toList())
				.orElseGet(List::of);
	}

	@Transactional
	public ChatSendResDto sendMessage(Long memberId, String content, MultipartFile image) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
		validateInput(content, image);

		ChatRoom chatRoom = chatRoomRepository.findByMember_MemberId(memberId)
				.orElseGet(() -> createChatRoomSafely(member));

		String normalizedContent = normalizeContent(content);
		String savedUserContent = hasText(normalizedContent) ? normalizedContent : IMAGE_ONLY_PLACEHOLDER;

		ChatMessage userMessage = chatMessageRepository.save(chatMapper.toUserMessage(chatRoom, savedUserContent));
		List<ChatMessage> messages = chatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(chatRoom.getChatRoomId());

		try {
			String assistantAnswer = generateAssistantAnswer(messages, normalizedContent, image);
			ChatMessage assistantMessage = chatMessageRepository.save(chatMapper.toAssistantMessage(chatRoom, assistantAnswer));

			return new ChatSendResDto(
					chatMapper.toChatMessageResDto(userMessage),
					chatMapper.toChatMessageResDto(assistantMessage)
			);
		} catch (GeneralException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new GeneralException(ChatErrorCode.CHAT_RESPONSE_FAILED);
		}
	}

	@Transactional
	public void deleteMessages(Long memberId) {
		validateMember(memberId);
		chatRoomRepository.findByMember_MemberId(memberId)
				.ifPresent(chatMessageRepository::deleteAllByChatRoom);
	}

	private String generateAssistantAnswer(List<ChatMessage> messages, String currentContent, MultipartFile image) {
		List<Message> promptMessages = new ArrayList<>();
		promptMessages.add(new SystemMessage(SYSTEM_PROMPT));

		for (int index = 0; index < messages.size() - 1; index++) {
			ChatMessage message = messages.get(index);
			if (message.getSenderType() == MessageSenderType.USER) {
				promptMessages.add(new UserMessage(message.getContent()));
			} else {
				promptMessages.add(new AssistantMessage(message.getContent()));
			}
		}

		promptMessages.add(buildCurrentUserMessage(currentContent, image));

		String responseText = chatModel.call(
						new Prompt(promptMessages, OpenAiChatOptions.builder().model(openAiModel).build()))
				.getResult()
				.getOutput()
				.getText();

		if (!hasText(responseText)) {
			throw new GeneralException(ChatErrorCode.CHAT_RESPONSE_FAILED);
		}

		return responseText.trim();
	}

	private UserMessage buildCurrentUserMessage(String content, MultipartFile image) {
		String promptText = hasText(content)
				? content
				: "첨부한 이미지를 보고 세탁 상담을 해줘.";

		if (image == null || image.isEmpty()) {
			return new UserMessage(promptText);
		}

		return UserMessage.builder()
				.text(promptText)
				.media(new Media(resolveMimeType(image), image.getResource()))
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

	private void validateInput(String content, MultipartFile image) {
		if (!hasText(content) && (image == null || image.isEmpty())) {
			throw new GeneralException(ChatErrorCode.CHAT_INPUT_REQUIRED);
		}

		if (image == null || image.isEmpty()) {
			return;
		}

		String contentType = image.getContentType();
		try {
			MimeType mimeType = MimeType.valueOf(contentType == null ? "" : contentType);
			if (!"image".equalsIgnoreCase(mimeType.getType())) {
				throw new GeneralException(ChatErrorCode.INVALID_IMAGE_TYPE);
			}
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ChatErrorCode.INVALID_IMAGE_TYPE);
		}
	}

	private MimeType resolveMimeType(MultipartFile image) {
		String contentType = image.getContentType();
		return contentType == null ? MimeTypeUtils.IMAGE_JPEG : MimeType.valueOf(contentType);
	}

	private String normalizeContent(String content) {
		return content == null ? null : content.trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

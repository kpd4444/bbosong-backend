package com.posong.ai_laundry.domain.chat.controller;

import com.posong.ai_laundry.domain.chat.dto.ChatMessageResDto;
import com.posong.ai_laundry.domain.chat.dto.ChatSendResDto;
import com.posong.ai_laundry.domain.chat.service.ChatService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Chat", description = "세탁 상담 채팅 API")
@RequiredArgsConstructor
@RequestMapping("/api/chat/messages")
public class ChatController {

	private final ChatService chatService;

	@Operation(
			summary = "채팅 메시지 조회",
			description = "로그인한 사용자의 채팅 메시지 전체를 오래된 순서대로 조회합니다. 채팅방이 없으면 빈 목록을 반환합니다."
	)
	@GetMapping
	public ApiResponse<List<ChatMessageResDto>> getMessages(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(chatService.getMessages(memberId));
	}

	@Operation(
			summary = "채팅 메시지 전송",
			description = "텍스트 메시지, 이미지, 텍스트와 이미지 조합을 모두 전송할 수 있습니다. 채팅방이 없으면 자동 생성되며, 이전 대화 이력을 포함해 Spring AI 응답을 생성합니다."
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ChatSendResDto> sendMessage(
			@AuthenticationPrincipal Long memberId,
			@Parameter(description = "사용자가 전송할 채팅 메시지입니다. 이미지 없이 텍스트만 보내도 됩니다.", example = "이 니트는 어떻게 세탁해야 해?")
			@RequestPart(value = "content", required = false) String content,
			@Parameter(description = "상담에 함께 보낼 이미지 파일입니다. 텍스트 없이 이미지만 보내도 됩니다.")
			@RequestPart(value = "image", required = false) MultipartFile image
	) {
		return ApiResponse.success(chatService.sendMessage(memberId, content, image));
	}

	@Operation(
			summary = "채팅 전체 삭제",
			description = "로그인한 사용자의 채팅 메시지 전체를 삭제합니다. 채팅방은 유지하고 메시지만 비웁니다."
	)
	@DeleteMapping
	public ApiResponse<Void> deleteMessages(@AuthenticationPrincipal Long memberId) {
		chatService.deleteMessages(memberId);
		return ApiResponse.success();
	}
}

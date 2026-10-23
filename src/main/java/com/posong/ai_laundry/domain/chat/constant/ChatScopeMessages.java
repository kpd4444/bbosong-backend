package com.posong.ai_laundry.domain.chat.constant;

import java.util.List;

/** 세탁 상담 범위 밖 질문에 사용하는 고정 응답과 추천 질문이다. */
public final class ChatScopeMessages {

	public static final String REJECTION_MESSAGE =
			"저는 세탁 전문 뽀송이라 그 질문은 도와드리기 어려워요 🫧 옷에 얼룩이 생겼거나 세탁 방법이 궁금하면 물어봐 주세요!";

	public static final List<String> SUGGESTED_QUESTIONS = List.of(
			"김칫국물이 튀었어요",
			"니트 세탁법",
			"흰 옷이 누렇게 변했어요"
	);

	private ChatScopeMessages() {
	}
}

package com.posong.ai_laundry.domain.chat.constant;

/** 범위 분류 판정 근거. LLM이 사용자 문장을 옮겨 적지 못하도록 자유 문장 대신 카테고리로만 받는다. */
public enum ChatScopeReason {
	// 범위 안
	LAUNDRY_CARE,
	FOLLOW_UP,
	GREETING,
	// 범위 밖
	UNRELATED_TOPIC,
	UNVERIFIABLE_INFO,
	PROMPT_INJECTION,
	OTHER
}

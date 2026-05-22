package com.posong.ai_laundry.domain.clothes.constant;

import java.util.Arrays;

public enum ClothesCategory {

	TOP("상의", new String[]{"상의", "티셔츠", "반팔", "긴팔", "셔츠", "블라우스", "니트", "맨투맨", "후드티", "조끼", "슬리브리스"}),
	OUTER("아우터", new String[]{"아우터", "자켓", "재킷", "코트", "패딩", "가디건", "점퍼", "후드집업"}),
	BOTTOM("하의", new String[]{"하의", "바지", "청바지", "데님", "슬랙스", "반바지", "레깅스", "조거"}),
	ONE_PIECE_SET("원피스/세트", new String[]{"원피스", "드레스", "세트", "투피스"}),
	INNERWEAR("이너웨어", new String[]{"이너웨어", "속옷", "브라", "팬티", "런닝", "내의", "잠옷", "파자마"}),
	TRAINING("트레이닝", new String[]{"트레이닝", "운동복", "트레이닝복", "레시가드", "스포츠웨어"}),
	HAT("모자", new String[]{"모자", "캡", "비니", "버킷햇"}),
	SCARF_MUFFLER("스카프/머플러", new String[]{"스카프", "머플러"}),
	SOCKS("양말", new String[]{"양말"}),
	GLOVES("장갑", new String[]{"장갑"}),
	BAG("가방", new String[]{"가방", "백팩", "크로스백", "토트백", "숄더백", "파우치"}),
	BEDDING("침구류", new String[]{"침구류", "이불", "담요", "베개커버", "시트"});

	private final String label;
	private final String[] keywords;

	ClothesCategory(String label, String[] keywords) {
		this.label = label;
		this.keywords = keywords;
	}

	public String label() {
		return label;
	}

	public static String normalize(String rawCategoryName) {
		if (rawCategoryName == null || rawCategoryName.isBlank()) {
			return null;
		}

		String normalized = rawCategoryName.trim().toLowerCase();

		return Arrays.stream(values())
				.filter(category -> Arrays.stream(category.keywords)
						.map(String::toLowerCase)
						.anyMatch(keyword -> normalized.equals(keyword) || normalized.contains(keyword)))
				.findFirst()
				.map(ClothesCategory::label)
				.orElse(null);
	}
}

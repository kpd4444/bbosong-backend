package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.dto.ClothesHomeResDto;
import com.posong.ai_laundry.domain.clothes.entity.Category;
import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import com.posong.ai_laundry.domain.clothes.mapper.ClothesMapper;
import com.posong.ai_laundry.domain.clothes.repository.CategoryRepository;
import com.posong.ai_laundry.domain.clothes.repository.ClothesRepository;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import com.posong.ai_laundry.global.storage.ImageUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClothesServiceTest {

	@Mock
	private ClothesRepository clothesRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ImageUrlResolver imageUrlResolver;

	@Mock
	private ImageStorageService imageStorageService;

	private ClothesService clothesService;

	@BeforeEach
	void setUp() {
		ClothesMapper clothesMapper = new ClothesMapper(imageUrlResolver);
		clothesService = new ClothesService(
				clothesRepository,
				categoryRepository,
				memberRepository,
				clothesMapper,
				imageStorageService
		);
	}

	@Test
	void getHomeClothesReturnsRecentAndFavoriteTopFive() {
		Long memberId = 1L;
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(Member.builder().nickname("tester").build()));

		List<Clothes> recentClothes = List.of(
				mockClothes(10L, "상의", "셔츠10", false, LocalDateTime.of(2026, 5, 17, 10, 0)),
				mockClothes(9L, "하의", "팬츠9", false, LocalDateTime.of(2026, 5, 17, 9, 0))
		);
		List<Clothes> favoriteClothes = List.of(
				mockClothes(8L, "아우터", "자켓8", true, LocalDateTime.of(2026, 5, 17, 8, 0))
		);

		when(clothesRepository.findTop5ByMember_MemberIdOrderByCreatedAtDesc(memberId)).thenReturn(recentClothes);
		when(clothesRepository.findTop5ByMember_MemberIdAndIsFavoriteTrueOrderByCreatedAtDesc(memberId))
				.thenReturn(favoriteClothes);

		ClothesHomeResDto result = clothesService.getHomeClothes(memberId);

		assertThat(result.recentClothes()).hasSize(2);
		assertThat(result.recentClothes().getFirst().clothesId()).isEqualTo(10L);
		assertThat(result.favoriteClothes()).hasSize(1);
		assertThat(result.favoriteClothes().getFirst().isFavorite()).isTrue();
	}

	private Clothes mockClothes(
			Long clothesId,
			String categoryName,
			String name,
			boolean isFavorite,
			LocalDateTime createdAt
	) {
		Clothes clothes = mock(Clothes.class);
		Category category = mock(Category.class);

		when(category.getName()).thenReturn(categoryName);
		when(clothes.getClothesId()).thenReturn(clothesId);
		when(clothes.getCategory()).thenReturn(category);
		when(clothes.getName()).thenReturn(name);
		when(clothes.getColor()).thenReturn("화이트");
		when(clothes.getImageKey()).thenReturn("clothes/2026/05/" + clothesId + ".jpg");
		when(imageUrlResolver.resolve("clothes/2026/05/" + clothesId + ".jpg"))
				.thenReturn("https://example.com/" + clothesId + ".jpg");
		when(clothes.isFavorite()).thenReturn(isFavorite);
		when(clothes.getCreatedAt()).thenReturn(createdAt);

		return clothes;
	}
}

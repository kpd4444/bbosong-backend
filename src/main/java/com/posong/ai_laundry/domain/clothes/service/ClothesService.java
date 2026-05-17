package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesCategory;
import com.posong.ai_laundry.domain.clothes.dto.ClothesDetailResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesFavoriteReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesFavoriteResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesHomeResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSummaryResDto;
import com.posong.ai_laundry.domain.clothes.entity.Category;
import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.domain.clothes.mapper.ClothesMapper;
import com.posong.ai_laundry.domain.clothes.repository.CategoryRepository;
import com.posong.ai_laundry.domain.clothes.repository.ClothesRepository;
import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesService {

	private final ClothesRepository clothesRepository;
	private final CategoryRepository categoryRepository;
	private final MemberRepository memberRepository;
	private final ClothesMapper clothesMapper;

	@Transactional
	public ClothesSaveResDto save(Long memberId, ClothesSaveReqDto request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));

		String normalizedCategoryName = normalizeCategoryName(request.categoryName());
		Category category = categoryRepository.findByName(normalizedCategoryName)
				.orElseGet(() -> createCategorySafely(normalizedCategoryName));

		Clothes clothes = clothesRepository.save(clothesMapper.toClothes(member, category, request));
		return clothesMapper.toClothesSaveResDto(clothes);
	}

	public List<ClothesSummaryResDto> getClothes(Long memberId, String categoryName) {
		validateMember(memberId);

		List<Clothes> clothesList = hasText(categoryName)
				? clothesRepository.findAllByMember_MemberIdAndCategory_NameOrderByCreatedAtDesc(
						memberId, normalizeCategoryName(categoryName))
				: clothesRepository.findAllByMember_MemberIdOrderByCreatedAtDesc(memberId);

		return clothesList.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();
	}

	public ClothesDetailResDto getClothesDetail(Long memberId, Long clothesId) {
		Clothes clothes = getOwnedClothes(memberId, clothesId);
		return clothesMapper.toClothesDetailResDto(clothes);
	}

	@Transactional
	public void delete(Long memberId, Long clothesId) {
		Clothes clothes = getOwnedClothes(memberId, clothesId);
		clothesRepository.delete(clothes);
	}

	public List<ClothesSummaryResDto> search(Long memberId, String categoryName, String keyword) {
		validateMember(memberId);
		String normalizedKeyword = keyword == null ? null : keyword.trim();
		if (!hasText(normalizedKeyword)) {
			return List.of();
		}

		List<Clothes> clothesList = hasText(categoryName)
				? clothesRepository.findAllByMember_MemberIdAndCategory_NameAndNameContainingOrderByCreatedAtDesc(
						memberId, normalizeCategoryName(categoryName), normalizedKeyword)
				: clothesRepository.findAllByMember_MemberIdAndNameContainingOrderByCreatedAtDesc(memberId, normalizedKeyword);

		return clothesList.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();
	}

	@Transactional
	public ClothesFavoriteResDto setFavorite(Long memberId, Long clothesId, ClothesFavoriteReqDto request) {
		Clothes clothes = getOwnedClothes(memberId, clothesId);
		try {
			if (clothes.isFavorite() != request.favorite()) {
				clothes.setFavorite(request.favorite());
			}
			Clothes savedClothes = clothesRepository.saveAndFlush(clothes);
			return clothesMapper.toClothesFavoriteResDto(savedClothes);
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new GeneralException(ClothesErrorCode.FAVORITE_CONFLICT);
		}
	}

	public List<ClothesSummaryResDto> getFavorites(Long memberId) {
		validateMember(memberId);
		return clothesRepository.findAllByMember_MemberIdAndIsFavoriteTrueOrderByCreatedAtDesc(memberId)
				.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();
	}

	public ClothesHomeResDto getHomeClothes(Long memberId) {
		validateMember(memberId);

		List<ClothesSummaryResDto> recentClothes = clothesRepository.findTop5ByMember_MemberIdOrderByCreatedAtDesc(memberId)
				.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();
		List<ClothesSummaryResDto> favoriteClothes = clothesRepository
				.findTop5ByMember_MemberIdAndIsFavoriteTrueOrderByCreatedAtDesc(memberId)
				.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();

		return new ClothesHomeResDto(recentClothes, favoriteClothes);
	}

	private Clothes getOwnedClothes(Long memberId, Long clothesId) {
		return clothesRepository.findByClothesIdAndMember_MemberId(clothesId, memberId)
				.orElseThrow(() -> new GeneralException(ClothesErrorCode.CLOTHES_NOT_FOUND));
	}

	private void validateMember(Long memberId) {
		memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private String normalizeCategoryName(String categoryName) {
		if (!hasText(categoryName)) {
			throw new GeneralException(ClothesErrorCode.CATEGORY_REQUIRED);
		}

		String normalizedCategoryName = ClothesCategory.normalize(categoryName);
		if (!hasText(normalizedCategoryName)) {
			throw new GeneralException(ClothesErrorCode.INVALID_CATEGORY);
		}

		return normalizedCategoryName;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private Category createCategorySafely(String categoryName) {
		try {
			return categoryRepository.save(Category.builder().name(categoryName).build());
		} catch (DataIntegrityViolationException exception) {
			return categoryRepository.findByName(categoryName).orElseThrow(() -> exception);
		}
	}
}

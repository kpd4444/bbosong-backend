package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesCategory;
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
				.orElseGet(() -> categoryRepository.save(Category.builder().name(normalizedCategoryName).build()));

		Clothes clothes = clothesRepository.save(clothesMapper.toClothes(member, category, request));
		return clothesMapper.toClothesSaveResDto(clothes);
	}

	public List<ClothesSummaryResDto> getClothes(Long memberId, String categoryName) {
		memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));

		List<Clothes> clothesList = hasText(categoryName)
				? clothesRepository.findAllByMember_MemberIdAndCategory_NameOrderByCreatedAtDesc(
						memberId, normalizeCategoryName(categoryName))
				: clothesRepository.findAllByMember_MemberIdOrderByCreatedAtDesc(memberId);

		return clothesList.stream()
				.map(clothesMapper::toClothesSummaryResDto)
				.toList();
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
}

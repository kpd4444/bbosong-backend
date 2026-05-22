package com.posong.ai_laundry.domain.store.service;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.member.exception.MemberErrorCode;
import com.posong.ai_laundry.domain.member.repository.MemberRepository;
import com.posong.ai_laundry.domain.store.dto.StoreFavoriteResDto;
import com.posong.ai_laundry.domain.store.dto.StoreFavoriteSaveReqDto;
import com.posong.ai_laundry.domain.store.entity.Store;
import com.posong.ai_laundry.domain.store.entity.StoreFavorite;
import com.posong.ai_laundry.domain.store.exception.StoreErrorCode;
import com.posong.ai_laundry.domain.store.mapper.StoreFavoriteMapper;
import com.posong.ai_laundry.domain.store.repository.StoreFavoriteRepository;
import com.posong.ai_laundry.domain.store.repository.StoreRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFavoriteService {

	private final StoreRepository storeRepository;
	private final StoreFavoriteRepository storeFavoriteRepository;
	private final MemberRepository memberRepository;
	private final StoreFavoriteMapper storeFavoriteMapper;

	@Transactional
	public StoreFavoriteResDto saveFavorite(Long memberId, StoreFavoriteSaveReqDto request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));

		storeFavoriteRepository.findByMember_MemberIdAndStore_KakaoPlaceId(memberId, request.kakaoPlaceId())
				.ifPresent(storeFavorite -> {
					throw new GeneralException(StoreErrorCode.STORE_FAVORITE_ALREADY_EXISTS);
				});

		Store store = storeRepository.findByKakaoPlaceId(request.kakaoPlaceId())
				.map(existingStore -> syncStore(existingStore, request))
				.orElseGet(() -> createStoreSafely(request));

		try {
			StoreFavorite savedFavorite = storeFavoriteRepository
					.saveAndFlush(storeFavoriteMapper.toStoreFavorite(member, store));
			return storeFavoriteMapper.toStoreFavoriteResDto(savedFavorite);
		} catch (DataIntegrityViolationException exception) {
			throw new GeneralException(StoreErrorCode.STORE_FAVORITE_ALREADY_EXISTS);
		}
	}

	public List<StoreFavoriteResDto> getFavorites(Long memberId) {
		validateMember(memberId);

		return storeFavoriteRepository.findAllByMember_MemberIdOrderByCreatedAtDesc(memberId)
				.stream()
				.map(storeFavoriteMapper::toStoreFavoriteResDto)
				.toList();
	}

	@Transactional
	public void deleteFavorite(Long memberId, Long storeId) {
		StoreFavorite storeFavorite = storeFavoriteRepository.findByMember_MemberIdAndStore_StoreId(memberId, storeId)
				.orElseThrow(() -> new GeneralException(StoreErrorCode.STORE_FAVORITE_NOT_FOUND));

		storeFavoriteRepository.delete(storeFavorite);
	}

	private void validateMember(Long memberId) {
		memberRepository.findById(memberId)
				.orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private Store createStoreSafely(StoreFavoriteSaveReqDto request) {
		try {
			return storeRepository.save(storeFavoriteMapper.toStore(request));
		} catch (DataIntegrityViolationException exception) {
			return storeRepository.findByKakaoPlaceId(request.kakaoPlaceId())
					.map(existingStore -> syncStore(existingStore, request))
					.orElseThrow(() -> exception);
		}
	}

	private Store syncStore(Store store, StoreFavoriteSaveReqDto request) {
		store.updateDetails(
				request.name(),
				request.address(),
				request.phone(),
				request.latitude(),
				request.longitude(),
				request.placeUrl()
		);
		return store;
	}
}

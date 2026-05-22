package com.posong.ai_laundry.domain.store.service;

import com.posong.ai_laundry.domain.member.entity.Member;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreFavoriteServiceTest {

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private StoreFavoriteRepository storeFavoriteRepository;

	@Mock
	private MemberRepository memberRepository;

	@Spy
	private StoreFavoriteMapper storeFavoriteMapper = new StoreFavoriteMapper();

	@InjectMocks
	private StoreFavoriteService storeFavoriteService;

	@Test
	void saveFavoriteCreatesMemberFavorite() {
		Long memberId = 1L;
		StoreFavoriteSaveReqDto request = new StoreFavoriteSaveReqDto(
				"123456789",
				"크린토피아",
				"인천 연수구",
				"032-123-4567",
				new BigDecimal("37.1234567"),
				new BigDecimal("127.1234567"),
				"https://place.map.kakao.com/123456789"
		);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(Member.builder().nickname("tester").build()));
		when(storeFavoriteRepository.findByMember_MemberIdAndStore_KakaoPlaceId(memberId, request.kakaoPlaceId()))
				.thenReturn(Optional.empty());
		when(storeRepository.findByKakaoPlaceId(request.kakaoPlaceId())).thenReturn(Optional.empty());

		Store savedStore = mock(Store.class);
		when(savedStore.getStoreId()).thenReturn(10L);
		when(savedStore.getKakaoPlaceId()).thenReturn(request.kakaoPlaceId());
		when(savedStore.getName()).thenReturn(request.name());
		when(savedStore.getAddress()).thenReturn(request.address());
		when(savedStore.getPhone()).thenReturn(request.phone());
		when(savedStore.getLatitude()).thenReturn(request.latitude());
		when(savedStore.getLongitude()).thenReturn(request.longitude());
		when(savedStore.getPlaceUrl()).thenReturn(request.placeUrl());
		when(storeRepository.save(any(Store.class))).thenReturn(savedStore);

		StoreFavorite savedFavorite = mock(StoreFavorite.class);
		when(savedFavorite.getStore()).thenReturn(savedStore);
		when(savedFavorite.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 5, 18, 22, 0));
		when(storeFavoriteRepository.saveAndFlush(any(StoreFavorite.class))).thenReturn(savedFavorite);

		StoreFavoriteResDto result = storeFavoriteService.saveFavorite(memberId, request);

		assertThat(result.storeId()).isEqualTo(10L);
		assertThat(result.kakaoPlaceId()).isEqualTo(request.kakaoPlaceId());
		assertThat(result.name()).isEqualTo(request.name());
	}

	@Test
	void saveFavoriteThrowsWhenAlreadyFavorited() {
		Long memberId = 1L;
		StoreFavoriteSaveReqDto request = new StoreFavoriteSaveReqDto(
				"123456789",
				"크린토피아",
				"인천 연수구",
				"032-123-4567",
				new BigDecimal("37.1234567"),
				new BigDecimal("127.1234567"),
				"https://place.map.kakao.com/123456789"
		);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(Member.builder().nickname("tester").build()));
		when(storeFavoriteRepository.findByMember_MemberIdAndStore_KakaoPlaceId(memberId, request.kakaoPlaceId()))
				.thenReturn(Optional.of(mock(StoreFavorite.class)));

		assertThatThrownBy(() -> storeFavoriteService.saveFavorite(memberId, request))
				.isInstanceOfSatisfying(GeneralException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(StoreErrorCode.STORE_FAVORITE_ALREADY_EXISTS));
	}

	@Test
	void getFavoritesReturnsFavoritedStores() {
		Long memberId = 1L;
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(Member.builder().nickname("tester").build()));

		Store store = mock(Store.class);
		when(store.getStoreId()).thenReturn(7L);
		when(store.getKakaoPlaceId()).thenReturn("7654321");
		when(store.getName()).thenReturn("런드리24");
		when(store.getAddress()).thenReturn("서울 성동구");
		when(store.getPhone()).thenReturn("02-123-4567");
		when(store.getLatitude()).thenReturn(new BigDecimal("37.5000000"));
		when(store.getLongitude()).thenReturn(new BigDecimal("127.0000000"));
		when(store.getPlaceUrl()).thenReturn("https://place.map.kakao.com/7654321");

		StoreFavorite storeFavorite = mock(StoreFavorite.class);
		when(storeFavorite.getStore()).thenReturn(store);
		when(storeFavorite.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 5, 18, 21, 0));

		when(storeFavoriteRepository.findAllByMember_MemberIdOrderByCreatedAtDesc(memberId))
				.thenReturn(List.of(storeFavorite));

		List<StoreFavoriteResDto> result = storeFavoriteService.getFavorites(memberId);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().storeId()).isEqualTo(7L);
		assertThat(result.getFirst().name()).isEqualTo("런드리24");
	}
}

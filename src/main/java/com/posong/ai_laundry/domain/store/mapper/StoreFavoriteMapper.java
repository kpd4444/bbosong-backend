package com.posong.ai_laundry.domain.store.mapper;

import com.posong.ai_laundry.domain.member.entity.Member;
import com.posong.ai_laundry.domain.store.dto.StoreFavoriteResDto;
import com.posong.ai_laundry.domain.store.dto.StoreFavoriteSaveReqDto;
import com.posong.ai_laundry.domain.store.entity.Store;
import com.posong.ai_laundry.domain.store.entity.StoreFavorite;
import org.springframework.stereotype.Component;

@Component
public class StoreFavoriteMapper {

	public Store toStore(StoreFavoriteSaveReqDto request) {
		return Store.builder()
				.kakaoPlaceId(request.kakaoPlaceId())
				.name(request.name())
				.address(request.address())
				.phone(request.phone())
				.latitude(request.latitude())
				.longitude(request.longitude())
				.placeUrl(request.placeUrl())
				.build();
	}

	public StoreFavorite toStoreFavorite(Member member, Store store) {
		return StoreFavorite.builder()
				.member(member)
				.store(store)
				.build();
	}

	public StoreFavoriteResDto toStoreFavoriteResDto(StoreFavorite storeFavorite) {
		Store store = storeFavorite.getStore();
		return new StoreFavoriteResDto(
				store.getStoreId(),
				store.getKakaoPlaceId(),
				store.getName(),
				store.getAddress(),
				store.getPhone(),
				store.getLatitude(),
				store.getLongitude(),
				store.getPlaceUrl(),
				storeFavorite.getCreatedAt()
		);
	}
}

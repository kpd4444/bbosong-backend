package com.posong.ai_laundry.domain.store.repository;

import com.posong.ai_laundry.domain.store.entity.StoreFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreFavoriteRepository extends JpaRepository<StoreFavorite, Long> {

	Optional<StoreFavorite> findByMember_MemberIdAndStore_KakaoPlaceId(Long memberId, String kakaoPlaceId);

	Optional<StoreFavorite> findByMember_MemberIdAndStore_StoreId(Long memberId, Long storeId);

	List<StoreFavorite> findAllByMember_MemberIdOrderByCreatedAtDesc(Long memberId);
}

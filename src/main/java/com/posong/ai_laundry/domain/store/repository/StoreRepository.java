package com.posong.ai_laundry.domain.store.repository;

import com.posong.ai_laundry.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

	Optional<Store> findByKakaoPlaceId(String kakaoPlaceId);
}

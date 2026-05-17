package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

	List<Clothes> findAllByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

	List<Clothes> findTop5ByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

	List<Clothes> findAllByMember_MemberIdAndCategory_NameOrderByCreatedAtDesc(Long memberId, String categoryName);

	List<Clothes> findAllByMember_MemberIdAndNameContainingOrderByCreatedAtDesc(Long memberId, String keyword);

	List<Clothes> findAllByMember_MemberIdAndCategory_NameAndNameContainingOrderByCreatedAtDesc(
			Long memberId, String categoryName, String keyword
	);

	List<Clothes> findAllByMember_MemberIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long memberId);

	List<Clothes> findTop5ByMember_MemberIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long memberId);

	Optional<Clothes> findByClothesIdAndMember_MemberId(Long clothesId, Long memberId);
}

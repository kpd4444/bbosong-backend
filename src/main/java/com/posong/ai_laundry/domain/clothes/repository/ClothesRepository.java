package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

	List<Clothes> findAllByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

	List<Clothes> findAllByMember_MemberIdAndCategory_NameOrderByCreatedAtDesc(Long memberId, String categoryName);
}

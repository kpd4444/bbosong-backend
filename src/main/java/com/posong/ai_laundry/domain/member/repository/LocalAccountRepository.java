package com.posong.ai_laundry.domain.member.repository;

import com.posong.ai_laundry.domain.member.entity.LocalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

	boolean existsByLoginId(String loginId);

	Optional<LocalAccount> findByLoginId(String loginId);

	void deleteByMember_MemberId(Long memberId);
}

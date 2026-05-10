package com.posong.ai_laundry.domain.member.repository;

import com.posong.ai_laundry.domain.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByMember_MemberId(Long memberId);

	Optional<RefreshToken> findByToken(String token);

	void deleteByMember_MemberId(Long memberId);
}

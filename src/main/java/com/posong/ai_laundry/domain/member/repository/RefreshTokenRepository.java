package com.posong.ai_laundry.domain.member.repository;

import com.posong.ai_laundry.domain.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByMember_MemberId(Long memberId);

	Optional<RefreshToken> findByToken(String token);

	void deleteByMember_MemberId(Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshToken refreshToken
			set refreshToken.token = :newToken,
			    refreshToken.expiresAt = :expiresAt
			where refreshToken.member.memberId = :memberId
			  and refreshToken.token = :currentToken
			""")
	int rotateToken(
			@Param("memberId") Long memberId,
			@Param("currentToken") String currentToken,
			@Param("newToken") String newToken,
			@Param("expiresAt") LocalDateTime expiresAt
	);
}

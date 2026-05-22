package com.posong.ai_laundry.domain.member.repository;

import com.posong.ai_laundry.domain.member.constant.SocialProvider;
import com.posong.ai_laundry.domain.member.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

	void deleteByMember_MemberId(Long memberId);
}

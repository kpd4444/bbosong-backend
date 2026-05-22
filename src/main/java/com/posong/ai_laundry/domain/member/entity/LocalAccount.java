package com.posong.ai_laundry.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "local_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "local_account_id")
	private Long localAccountId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@Column(name = "login_id", nullable = false, unique = true, length = 100)
	private String loginId;

	@Column(nullable = false, length = 255)
	private String password;

	@Builder
	private LocalAccount(Member member, String loginId, String password) {
		this.member = member;
		this.loginId = loginId;
		this.password = password;
	}
}

package com.posong.ai_laundry.domain.store.entity;

import com.posong.ai_laundry.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
		name = "store_favorite",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_store_favorite_member_store",
						columnNames = {"member_id", "store_id"}
				)
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreFavorite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "store_favorite_id")
	private Long storeFavoriteId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Builder
	private StoreFavorite(Member member, Store store) {
		this.member = member;
		this.store = store;
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}

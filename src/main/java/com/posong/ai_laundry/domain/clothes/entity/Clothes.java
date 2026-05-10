package com.posong.ai_laundry.domain.clothes.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "clothes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "clothes_id")
	private Long clothesId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String material;

	@Column(nullable = false, length = 100)
	private String color;

	@Column(name = "washing_method", nullable = false, length = 255)
	private String washingMethod;

	@Column(nullable = false, length = 500)
	private String caution;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "is_favorite", nullable = false)
	private boolean isFavorite;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	private Clothes(Member member, Category category, String name, String material, String color,
				 String washingMethod, String caution, String imageUrl, boolean isFavorite) {
		this.member = member;
		this.category = category;
		this.name = name;
		this.material = material;
		this.color = color;
		this.washingMethod = washingMethod;
		this.caution = caution;
		this.imageUrl = imageUrl;
		this.isFavorite = isFavorite;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void toggleFavorite() {
		this.isFavorite = !this.isFavorite;
	}
}

package com.posong.ai_laundry.domain.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "store")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "store_id")
	private Long storeId;

	@Column(name = "kakao_place_id", nullable = false, unique = true, length = 100)
	private String kakaoPlaceId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String address;

	@Column(length = 30)
	private String phone;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "place_url", length = 500)
	private String placeUrl;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	private Store(
			String kakaoPlaceId,
			String name,
			String address,
			String phone,
			BigDecimal latitude,
			BigDecimal longitude,
			String placeUrl
	) {
		this.kakaoPlaceId = kakaoPlaceId;
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.latitude = latitude;
		this.longitude = longitude;
		this.placeUrl = placeUrl;
	}

	public void updateDetails(
			String name,
			String address,
			String phone,
			BigDecimal latitude,
			BigDecimal longitude,
			String placeUrl
	) {
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.latitude = latitude;
		this.longitude = longitude;
		this.placeUrl = placeUrl;
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
}

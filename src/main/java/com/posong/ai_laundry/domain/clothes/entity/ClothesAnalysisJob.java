package com.posong.ai_laundry.domain.clothes.entity;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "clothes_analysis_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAnalysisJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "analysis_job_id")
	private Long analysisJobId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClothesAnalysisJobStatus status;

	@Column(name = "image_content_type", nullable = false, length = 100)
	private String imageContentType;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "image_data", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] imageData;

	@Column(name = "category_name", length = 100)
	private String categoryName;

	@Column(length = 100)
	private String name;

	@Column(length = 255)
	private String material;

	@Column(length = 100)
	private String color;

	@Column(name = "washing_method", length = 255)
	private String washingMethod;

	@Column(length = 500)
	private String caution;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	private ClothesAnalysisJob(Long memberId, String imageContentType, byte[] imageData) {
		this.memberId = memberId;
		this.imageContentType = imageContentType;
		this.imageData = imageData;
		this.status = ClothesAnalysisJobStatus.PENDING;
	}

	public static ClothesAnalysisJob create(Long memberId, String imageContentType, byte[] imageData) {
		return new ClothesAnalysisJob(memberId, imageContentType, imageData);
	}

	public void markProcessing() {
		this.status = ClothesAnalysisJobStatus.PROCESSING;
		this.errorMessage = null;
	}

	public void complete(ClothesAnalysisResDto result) {
		this.status = ClothesAnalysisJobStatus.SUCCESS;
		this.categoryName = result.categoryName();
		this.name = result.name();
		this.material = result.material();
		this.color = result.color();
		this.washingMethod = result.washingMethod();
		this.caution = result.caution();
		this.errorMessage = null;
		this.completedAt = LocalDateTime.now();
	}

	public void fail(String errorMessage) {
		this.status = ClothesAnalysisJobStatus.FAILED;
		this.errorMessage = truncate(errorMessage);
		this.completedAt = LocalDateTime.now();
	}

	public ClothesAnalysisResDto toResult() {
		if (status != ClothesAnalysisJobStatus.SUCCESS) {
			return null;
		}
		return new ClothesAnalysisResDto(categoryName, name, material, color, washingMethod, caution);
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

	private String truncate(String value) {
		if (value == null || value.length() <= 500) {
			return value;
		}
		return value.substring(0, 500);
	}
}

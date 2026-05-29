package com.posong.ai_laundry.domain.clothes.dto;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesAnalysisJobResDto(
		@Schema(description = "의류 분석 작업 ID", example = "1")
		Long jobId,

		@Schema(description = "의류 분석 작업 상태", example = "PENDING")
		ClothesAnalysisJobStatus status
) {
	public static ClothesAnalysisJobResDto from(ClothesAnalysisJob job) {
		return new ClothesAnalysisJobResDto(job.getAnalysisJobId(), job.getStatus());
	}
}

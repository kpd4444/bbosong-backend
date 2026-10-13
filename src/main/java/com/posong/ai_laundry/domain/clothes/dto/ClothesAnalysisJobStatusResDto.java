package com.posong.ai_laundry.domain.clothes.dto;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisFailure;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClothesAnalysisJobStatusResDto(
		@Schema(description = "의류 분석 작업 ID", example = "1")
		Long jobId,

		@Schema(description = "의류 분석 작업 상태", example = "SUCCESS")
		ClothesAnalysisJobStatus status,

		@Schema(description = "분석 성공 시 의류 분석 결과입니다.")
		ClothesAnalysisResDto result,

		@Schema(description = "분석 실패 시 실패 사유입니다.")
		String errorMessage,

		@Schema(description = "분석 실패 시 오류 코드입니다.", example = "CLOTHES_003")
		String errorCode
) {
	public static ClothesAnalysisJobStatusResDto from(ClothesAnalysisJob job) {
		ClothesAnalysisFailure failure = job.getStatus() == ClothesAnalysisJobStatus.FAILED
				? ClothesAnalysisFailure.fromStoredMessage(job.getErrorMessage()) : null;
		return new ClothesAnalysisJobStatusResDto(
				job.getAnalysisJobId(),
				job.getStatus(),
				job.toResult(),
				failure == null ? null : failure.getErrorCode().getMessage(),
				failure == null ? null : failure.getErrorCode().getCode()
		);
	}
}

package com.posong.ai_laundry.domain.clothes.controller;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobStatusResDto;
import com.posong.ai_laundry.domain.clothes.service.ClothesAnalysisAsyncService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Clothes Analysis", description = "Clothes image analysis API")
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesAnalysisController {

	private final ClothesAnalysisAsyncService clothesAnalysisAsyncService;

	@Operation(
			summary = "Submit clothes image analysis",
			description = "Creates an asynchronous clothes image analysis job and returns a job id."
	)
	@PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ClothesAnalysisJobResDto>> analyze(
			@AuthenticationPrincipal Long memberId,
			@Parameter(description = "Clothes image file to analyze.", required = true)
			@RequestPart("image") MultipartFile image
	) {
		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.success(clothesAnalysisAsyncService.submit(memberId, image)));
	}

	@Operation(
			summary = "Get clothes image analysis job",
			description = "Returns the current status and result of a clothes image analysis job."
	)
	@GetMapping("/analysis/{jobId}")
	public ApiResponse<ClothesAnalysisJobStatusResDto> getAnalysisJob(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long jobId
	) {
		return ApiResponse.success(clothesAnalysisAsyncService.getStatus(memberId, jobId));
	}
}

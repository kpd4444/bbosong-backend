package com.posong.ai_laundry.domain.clothes.controller;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.service.ClothesAnalysisService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Clothes Analysis", description = "의류 이미지 분석 API")
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesAnalysisController {

	private final ClothesAnalysisService clothesAnalysisService;

	@Operation(
			summary = "의류 이미지 분석",
			description = "업로드한 옷 사진을 분석해서 옷의 카테고리, 이름, 소재, 색상, 권장 세탁 방법, 세탁 시 주의사항을 반환합니다. "
					+ "사진 한 장만 업로드하면 되고, 분석 결과는 저장되지 않습니다. "
					+ "현재 단계에서는 AI가 사진을 보고 추정한 결과를 응답으로만 내려줍니다."
	)
	@PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ClothesAnalysisResDto> analyze(
			@Parameter(
					description = "분석할 의류 이미지 파일입니다. JPG, JPEG, PNG 같은 일반 이미지 파일을 업로드하면 됩니다. "
							+ "옷이 잘 보이도록 단일 의류 중심으로 촬영한 사진을 권장합니다.",
					required = true
			)
			@RequestPart("image") MultipartFile image
	) {
		return ApiResponse.success(clothesAnalysisService.analyze(image));
	}
}

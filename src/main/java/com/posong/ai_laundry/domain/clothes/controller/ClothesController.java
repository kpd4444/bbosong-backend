package com.posong.ai_laundry.domain.clothes.controller;

import com.posong.ai_laundry.domain.clothes.dto.ClothesDetailResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesFavoriteReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesFavoriteResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesHomeResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveReqDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSaveResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesSummaryResDto;
import com.posong.ai_laundry.domain.clothes.service.ClothesService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Clothes Closet", description = "의류 저장 및 옷장 조회 API")
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

	private final ClothesService clothesService;

	@Operation(
			summary = "의류 저장",
			description = "분석 결과를 확인한 뒤 의류 정보를 옷장에 저장합니다. 카테고리 값은 서버에서 정규화한 뒤 category 테이블과 연결합니다."
	)
	@PostMapping
	public ApiResponse<ClothesSaveResDto> save(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody ClothesSaveReqDto request
	) {
		return ApiResponse.success(clothesService.save(memberId, request));
	}

	@Operation(
			summary = "옷장 목록 조회",
			description = "로그인한 회원의 옷장 목록을 최신순으로 조회합니다. category 쿼리 파라미터를 주면 해당 카테고리만 조회합니다."
	)
	@GetMapping
	public ApiResponse<List<ClothesSummaryResDto>> getClothes(
			@AuthenticationPrincipal Long memberId,
			@Parameter(description = "조회할 카테고리입니다. 예: 상의, 하의, 아우터", example = "상의")
			@RequestParam(required = false) String category
	) {
		return ApiResponse.success(clothesService.getClothes(memberId, category));
	}

	@Operation(
			summary = "의류 상세 조회",
			description = "로그인한 회원이 저장한 특정 의류의 상세 정보를 조회합니다."
	)
	@GetMapping("/{clothesId}")
	public ApiResponse<ClothesDetailResDto> getClothesDetail(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long clothesId
	) {
		return ApiResponse.success(clothesService.getClothesDetail(memberId, clothesId));
	}

	@Operation(
			summary = "의류 삭제",
			description = "로그인한 회원이 저장한 특정 의류를 삭제합니다."
	)
	@DeleteMapping("/{clothesId}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long clothesId
	) {
		clothesService.delete(memberId, clothesId);
		return ApiResponse.success();
	}

	@Operation(
			summary = "의류 검색",
			description = "로그인한 회원의 옷장에서 이름 기준으로 의류를 검색합니다. category를 함께 주면 해당 카테고리 안에서만 검색합니다."
	)
	@GetMapping("/search")
	public ApiResponse<List<ClothesSummaryResDto>> search(
			@AuthenticationPrincipal Long memberId,
			@Parameter(description = "검색어입니다. 의류 이름 기준 부분 검색을 수행합니다.", example = "니트")
			@RequestParam String keyword,
			@Parameter(description = "카테고리 필터입니다. 없으면 전체 카테고리에서 검색합니다.", example = "상의")
			@RequestParam(required = false) String category
	) {
		return ApiResponse.success(clothesService.search(memberId, category, keyword));
	}

	@Operation(
			summary = "의류 즐겨찾기 토글",
			description = "로그인한 회원이 저장한 특정 의류의 즐겨찾기 상태를 토글합니다."
	)
	@PatchMapping("/{clothesId}/favorite")
	public ApiResponse<ClothesFavoriteResDto> setFavorite(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long clothesId,
			@Valid @RequestBody ClothesFavoriteReqDto request
	) {
		return ApiResponse.success(clothesService.setFavorite(memberId, clothesId, request));
	}

	@Operation(
			summary = "즐겨찾기 의류 조회",
			description = "로그인한 회원이 즐겨찾기한 의류 목록만 최신순으로 조회합니다."
	)
	@GetMapping("/favorites")
	public ApiResponse<List<ClothesSummaryResDto>> getFavorites(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(clothesService.getFavorites(memberId));
	}

	@Operation(
			summary = "홈 옷장 요약 조회",
			description = "로그인한 회원의 최근 등록 의류 5개와 즐겨찾기 의류 5개를 함께 조회합니다."
	)
	@GetMapping("/home")
	public ApiResponse<ClothesHomeResDto> getHomeClothes(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(clothesService.getHomeClothes(memberId));
	}
}

package com.posong.ai_laundry.domain.store.controller;

import com.posong.ai_laundry.domain.store.dto.StoreFavoriteResDto;
import com.posong.ai_laundry.domain.store.dto.StoreFavoriteSaveReqDto;
import com.posong.ai_laundry.domain.store.service.StoreFavoriteService;
import com.posong.ai_laundry.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Store Favorite", description = "즐겨찾기 매장 관리 API")
@RequiredArgsConstructor
@RequestMapping("/api/stores/favorites")
public class StoreFavoriteController {

	private final StoreFavoriteService storeFavoriteService;

	@Operation(summary = "즐겨찾기 매장 저장", description = "회원의 즐겨찾기 매장을 저장합니다.")
	@PostMapping
	public ApiResponse<StoreFavoriteResDto> saveFavorite(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody StoreFavoriteSaveReqDto request
	) {
		return ApiResponse.success(storeFavoriteService.saveFavorite(memberId, request));
	}

	@Operation(summary = "즐겨찾기 매장 목록 조회", description = "회원이 즐겨찾기한 매장 목록을 최신순으로 조회합니다.")
	@GetMapping
	public ApiResponse<List<StoreFavoriteResDto>> getFavorites(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(storeFavoriteService.getFavorites(memberId));
	}

	@Operation(summary = "즐겨찾기 매장 삭제", description = "회원이 즐겨찾기한 매장을 삭제합니다.")
	@DeleteMapping("/{storeId}")
	public ApiResponse<Void> deleteFavorite(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long storeId
	) {
		storeFavoriteService.deleteFavorite(memberId, storeId);
		return ApiResponse.success();
	}
}

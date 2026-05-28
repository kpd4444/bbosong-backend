package com.posong.ai_laundry.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record MemberBirthUpdateReqDto(
		@Schema(description = "변경할 생년월일", example = "2001-05-20")
		@NotNull(message = "생년월일은 필수입니다.")
		@PastOrPresent(message = "생년월일은 미래 날짜일 수 없습니다.")
		LocalDate birthDate
) {
}

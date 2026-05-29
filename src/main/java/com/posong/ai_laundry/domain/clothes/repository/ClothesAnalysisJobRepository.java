package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClothesAnalysisJobRepository extends JpaRepository<ClothesAnalysisJob, Long> {

	Optional<ClothesAnalysisJob> findByAnalysisJobIdAndMemberId(Long analysisJobId, Long memberId);

	List<ClothesAnalysisJob> findAllByStatus(ClothesAnalysisJobStatus status);
}

package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface ClothesAnalysisJobRepository extends JpaRepository<ClothesAnalysisJob, Long> {

	Optional<ClothesAnalysisJob> findByAnalysisJobIdAndMemberId(Long analysisJobId, Long memberId);

	List<ClothesAnalysisJob> findAllByStatus(ClothesAnalysisJobStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select j from ClothesAnalysisJob j where j.analysisJobId = :id")
	Optional<ClothesAnalysisJob> findByIdForUpdate(@Param("id") Long id);

	@Query("select j.analysisJobId from ClothesAnalysisJob j where j.status = :status order by j.analysisJobId")
	List<Long> findIdsByStatus(@Param("status") ClothesAnalysisJobStatus status, Pageable pageable);

	@Query("select j.analysisJobId from ClothesAnalysisJob j where j.status = :status and j.updatedAt < :cutoff order by j.analysisJobId")
	List<Long> findStaleIds(@Param("status") ClothesAnalysisJobStatus status,
			@Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}

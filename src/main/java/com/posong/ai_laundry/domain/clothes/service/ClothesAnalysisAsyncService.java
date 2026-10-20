package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisFailure;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobStatusResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.file.ImageFileSizeExceededException;
import com.posong.ai_laundry.global.file.ImageProcessor;
import com.posong.ai_laundry.global.file.ProcessedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ClothesAnalysisAsyncService {

	private final ClothesAnalysisJobRepository clothesAnalysisJobRepository;
	private final ClothesAnalysisService clothesAnalysisService;
	private final Executor clothesAnalysisTaskExecutor;
	private final TransactionTemplate transactionTemplate;
	private final Set<Long> submittedJobs = ConcurrentHashMap.newKeySet();

	@Value("${analysis-job.processing-timeout:10m}")
	private Duration processingTimeout = Duration.ofMinutes(10);

	@Value("${external-api.openai.call-timeout:60s}")
	private Duration apiTimeout = Duration.ofSeconds(60);

	@PostConstruct
	void validateTimeouts() {
		if (processingTimeout.isNegative() || processingTimeout.isZero()
				|| processingTimeout.compareTo(apiTimeout) <= 0) {
			throw new IllegalArgumentException("analysis-job.processing-timeout must exceed the OpenAI call timeout");
		}
	}

	public ClothesAnalysisAsyncService(
			ClothesAnalysisJobRepository clothesAnalysisJobRepository,
			ClothesAnalysisService clothesAnalysisService,
			@Qualifier("clothesAnalysisTaskExecutor") Executor clothesAnalysisTaskExecutor,
			TransactionTemplate transactionTemplate
	) {
		this.clothesAnalysisJobRepository = clothesAnalysisJobRepository;
		this.clothesAnalysisService = clothesAnalysisService;
		this.clothesAnalysisTaskExecutor = clothesAnalysisTaskExecutor;
		this.transactionTemplate = transactionTemplate;
	}

	public ClothesAnalysisJobResDto submit(Long memberId, MultipartFile image) {
		ProcessedImage processedImage = validateAndProcessImage(image);

		ClothesAnalysisJob job = clothesAnalysisJobRepository.saveAndFlush(
				ClothesAnalysisJob.create(memberId, processedImage.mimeType().toString(), processedImage.bytes())
		);

		enqueueOrFail(job.getAnalysisJobId(), true);

		return ClothesAnalysisJobResDto.from(job);
	}

	@Transactional(readOnly = true)
	public ClothesAnalysisJobStatusResDto getStatus(Long memberId, Long jobId) {
		ClothesAnalysisJob job = clothesAnalysisJobRepository.findByAnalysisJobIdAndMemberId(jobId, memberId)
				.orElseThrow(() -> new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_JOB_NOT_FOUND));
		return ClothesAnalysisJobStatusResDto.from(job);
	}

	public void process(Long jobId) {
		AnalysisJobPayload payload = transactionTemplate.execute(status -> markProcessing(jobId));
		if (payload == null) {
			log.debug("Skipped unclaimed analysis job. jobId={}", jobId);
			return;
		}

		try {
			ClothesAnalysisResDto result = clothesAnalysisService.analyze(
					payload.imageData(),
					MimeTypeUtils.parseMimeType(payload.imageContentType())
			);
			transactionTemplate.executeWithoutResult(status -> complete(jobId, result));
		} catch (Exception exception) {
			log.warn("Failed to process clothes analysis job. jobId={}", jobId, exception);
			transactionTemplate.executeWithoutResult(status -> fail(jobId, ClothesAnalysisJobStatus.PROCESSING,
					ClothesAnalysisFailure.fromException(exception)));
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(fixedDelayString = "${analysis-job.recovery-interval:60s}",
			initialDelayString = "${analysis-job.recovery-interval:60s}")
	public void recoverUnfinishedJobs() {
		LocalDateTime cutoff = LocalDateTime.now().minus(processingTimeout);
		List<Long> staleIds = clothesAnalysisJobRepository.findStaleIds(
				ClothesAnalysisJobStatus.PROCESSING, cutoff, PageRequest.of(0, 100));
		for (Long jobId : staleIds) {
			try {
				transactionTemplate.executeWithoutResult(status -> clothesAnalysisJobRepository.findByIdForUpdate(jobId)
						.filter(job -> job.getStatus() == ClothesAnalysisJobStatus.PROCESSING
								&& job.getUpdatedAt().isBefore(cutoff))
						.ifPresent(job -> {
							job.fail(ClothesAnalysisFailure.ANALYSIS_FAILED.getErrorCode().getMessage());
							log.warn("Expired analysis job. jobId={}, previousState=PROCESSING, reason=processing-timeout", jobId);
						}));
			} catch (RuntimeException exception) {
				log.warn("Could not expire analysis job. jobId={}", jobId, exception);
			}
		}
		List<Long> pendingJobIds = Objects.requireNonNull(transactionTemplate.execute(status ->
				clothesAnalysisJobRepository.findIdsByStatus(ClothesAnalysisJobStatus.PENDING, PageRequest.of(0, 100))
		));

		pendingJobIds.forEach(jobId -> enqueueOrFail(jobId, false));
		if (!pendingJobIds.isEmpty()) {
			log.info("Recovered pending clothes analysis jobs. count={}", pendingJobIds.size());
		}
	}

	private void enqueueOrFail(Long jobId, boolean throwOnRejected) {
		if (!submittedJobs.add(jobId)) {
			return;
		}
		try {
			clothesAnalysisTaskExecutor.execute(() -> {
				try {
					process(jobId);
				} finally {
					submittedJobs.remove(jobId);
				}
			});
		} catch (RejectedExecutionException exception) {
			submittedJobs.remove(jobId);
			log.warn("Clothes analysis task rejected. jobId={}", jobId, exception);
			if (throwOnRejected) {
				Boolean failed = transactionTemplate.execute(status -> fail(jobId, ClothesAnalysisJobStatus.PENDING,
						ClothesAnalysisFailure.QUEUE_FULL));
				if (Boolean.TRUE.equals(failed)) {
					throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_FULL);
				}
			}
		} catch (RuntimeException exception) {
			submittedJobs.remove(jobId);
			throw exception;
		}
	}

	private AnalysisJobPayload markProcessing(Long jobId) {
		ClothesAnalysisJob job = clothesAnalysisJobRepository.findByIdForUpdate(jobId).orElse(null);
		if (job == null || job.getStatus() != ClothesAnalysisJobStatus.PENDING) {
			return null;
		}
		job.markProcessing();
		log.info("Claimed analysis job. jobId={}, state=PROCESSING", jobId);
		return new AnalysisJobPayload(job.getImageContentType(), job.getImageData());
	}

	private void complete(Long jobId, ClothesAnalysisResDto result) {
		clothesAnalysisJobRepository.findByIdForUpdate(jobId)
				.filter(job -> job.getStatus() == ClothesAnalysisJobStatus.PROCESSING)
				.ifPresentOrElse(job -> {
					job.complete(result);
					log.info("Completed analysis job. jobId={}, state=SUCCESS", jobId);
				}, () -> log.info("Ignored late analysis result. jobId={}", jobId));
	}

	private boolean fail(Long jobId, ClothesAnalysisJobStatus expected, ClothesAnalysisFailure failure) {
		return clothesAnalysisJobRepository.findByIdForUpdate(jobId)
				.filter(job -> job.getStatus() == expected)
				.map(job -> {
					job.fail(failure.getErrorCode().getMessage());
					log.info("Failed analysis job. jobId={}, previousState={}, reason={}", jobId, expected, failure);
					return true;
				}).orElse(false);
	}

	private ProcessedImage validateAndProcessImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new GeneralException(ClothesErrorCode.IMAGE_REQUIRED);
		}

		try {
			return ImageProcessor.process(image);
		} catch (ImageFileSizeExceededException exception) {
			throw new GeneralException(GlobalErrorCode.FILE_SIZE_EXCEEDED);
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ClothesErrorCode.INVALID_IMAGE_TYPE);
		}
	}

	private record AnalysisJobPayload(String imageContentType, byte[] imageData) {
	}
}

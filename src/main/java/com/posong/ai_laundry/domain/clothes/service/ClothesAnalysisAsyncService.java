package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobStatusResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.file.ImageFileSizeExceededException;
import com.posong.ai_laundry.global.file.ImageFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ClothesAnalysisAsyncService {

	private final ClothesAnalysisJobRepository clothesAnalysisJobRepository;
	private final ClothesAnalysisService clothesAnalysisService;
	private final Executor clothesAnalysisTaskExecutor;
	private final TransactionTemplate transactionTemplate;

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
		MimeType imageMimeType = validateAndResolveImage(image);
		byte[] imageData = readImageBytes(image);

		ClothesAnalysisJob job = clothesAnalysisJobRepository.saveAndFlush(
				ClothesAnalysisJob.create(memberId, imageMimeType.toString(), imageData)
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
		AnalysisJobPayload payload = Objects.requireNonNull(
				transactionTemplate.execute(status -> markProcessing(jobId))
		);

		try {
			ClothesAnalysisResDto result = clothesAnalysisService.analyze(
					payload.imageData(),
					MimeTypeUtils.parseMimeType(payload.imageContentType())
			);
			transactionTemplate.executeWithoutResult(status -> complete(jobId, result));
		} catch (Exception exception) {
			log.warn("Failed to process clothes analysis job. jobId={}", jobId, exception);
			transactionTemplate.executeWithoutResult(status -> fail(jobId, exception.getMessage()));
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void recoverUnfinishedJobs() {
		List<Long> pendingJobIds = Objects.requireNonNull(transactionTemplate.execute(status ->
				clothesAnalysisJobRepository.findAllByStatus(ClothesAnalysisJobStatus.PENDING)
						.stream()
						.map(ClothesAnalysisJob::getAnalysisJobId)
						.toList()
		));

		pendingJobIds.forEach(jobId -> enqueueOrFail(jobId, false));
		if (!pendingJobIds.isEmpty()) {
			log.info("Recovered pending clothes analysis jobs. count={}", pendingJobIds.size());
		}
	}

	private void enqueueOrFail(Long jobId, boolean throwOnRejected) {
		try {
			clothesAnalysisTaskExecutor.execute(() -> process(jobId));
		} catch (TaskRejectedException exception) {
			transactionTemplate.executeWithoutResult(status -> fail(jobId, "Clothes analysis queue is full."));
			log.warn("Clothes analysis task rejected. jobId={}", jobId, exception);
			if (throwOnRejected) {
				throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_FULL);
			}
		}
	}

	private AnalysisJobPayload markProcessing(Long jobId) {
		ClothesAnalysisJob job = clothesAnalysisJobRepository.findById(jobId)
				.orElseThrow(() -> new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_JOB_NOT_FOUND));
		job.markProcessing();
		return new AnalysisJobPayload(job.getImageContentType(), job.getImageData());
	}

	private void complete(Long jobId, ClothesAnalysisResDto result) {
		ClothesAnalysisJob job = clothesAnalysisJobRepository.findById(jobId)
				.orElseThrow(() -> new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_JOB_NOT_FOUND));
		job.complete(result);
	}

	private void fail(Long jobId, String errorMessage) {
		clothesAnalysisJobRepository.findById(jobId)
				.ifPresent(job -> job.fail(errorMessage == null ? "Clothes analysis failed." : errorMessage));
	}

	private MimeType validateAndResolveImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new GeneralException(ClothesErrorCode.IMAGE_REQUIRED);
		}

		try {
			return ImageFileValidator.detectSupportedMimeType(image);
		} catch (ImageFileSizeExceededException exception) {
			throw new GeneralException(GlobalErrorCode.FILE_SIZE_EXCEEDED);
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ClothesErrorCode.INVALID_IMAGE_TYPE);
		}
	}

	private byte[] readImageBytes(MultipartFile image) {
		try {
			return image.getBytes();
		} catch (IOException exception) {
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private record AnalysisJobPayload(String imageContentType, byte[] imageData) {
	}
}

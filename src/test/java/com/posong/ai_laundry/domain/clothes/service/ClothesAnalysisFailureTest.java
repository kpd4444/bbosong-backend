package com.posong.ai_laundry.domain.clothes.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobStatusResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClothesAnalysisFailureTest {
	private ClothesAnalysisJobRepository repository;
	private ClothesAnalysisService analyzer;
	private ClothesAnalysisAsyncService service;
	private ClothesAnalysisJob job;
	private TransactionTemplate transactions;

	@BeforeEach
	void setUp() {
		repository = mock(ClothesAnalysisJobRepository.class);
		analyzer = mock(ClothesAnalysisService.class);
		PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
		when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
		transactions = new TransactionTemplate(manager);
		service = new ClothesAnalysisAsyncService(repository, analyzer, Runnable::run, transactions);
		job = ClothesAnalysisJob.create(1L, "image/png", new byte[]{1});
		when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(job));
		when(repository.findByAnalysisJobIdAndMemberId(7L, 1L)).thenReturn(Optional.of(job));
	}

	@Test
	void unexpectedFailureStoresSafeMessageAndLogsOriginalCause() {
		RuntimeException failure = new IllegalStateException("sensitive upstream response");
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenThrow(failure);
		Logger logger = (Logger) LoggerFactory.getLogger(ClothesAnalysisAsyncService.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			service.process(7L);
			assertFailure(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED);
			assertThat(appender.list).anySatisfy(event -> {
				assertThat(event.getFormattedMessage()).contains("jobId=7");
				assertThat(event.getThrowableProxy()).isNotNull();
				assertThat(event.getThrowableProxy().getMessage()).isEqualTo(failure.getMessage());
			});
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

	@Test
	void knownAnalysisFailureKeepsPublicCode() {
		when(analyzer.analyze(any(byte[].class), any(MimeType.class)))
				.thenThrow(new GeneralException(ClothesErrorCode.INVALID_ANALYSIS_RESULT));
		service.process(7L);
		assertFailure(ClothesErrorCode.INVALID_ANALYSIS_RESULT);
	}

	@Test
	void rejectedRecoveredJobRemainsPendingForNextSweep() {
		org.springframework.test.util.ReflectionTestUtils.setField(job, "analysisJobId", 7L);
		when(repository.findIdsByStatus(eq(ClothesAnalysisJobStatus.PENDING), any())).thenReturn(List.of(7L));
		service = new ClothesAnalysisAsyncService(repository, analyzer,
				command -> { throw new TaskRejectedException("executor internal details"); }, transactions);
		service.recoverUnfinishedJobs();
		assertThat(job.getStatus()).isEqualTo(ClothesAnalysisJobStatus.PENDING);
		verifyNoInteractions(analyzer);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"database password=secret", "upstream stack trace", ""})
	void legacyInternalMessagesAreHiddenOnStatusRead(String message) {
		job.fail(message);
		ClothesAnalysisJobStatusResDto response = service.getStatus(1L, 7L);
		assertThat(response.errorCode()).isEqualTo("CLOTHES_003");
		assertThat(response.errorMessage()).isEqualTo(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED.getMessage());
	}

	@Test
	void legacyQueueMessageRetainsQueueCode() {
		job.fail("Clothes analysis queue is full.");
		assertThat(service.getStatus(1L, 7L).errorCode()).isEqualTo("CLOTHES_010");
	}

	@Test
	void nonFailedStatesDoNotExposeFailureFields() {
		assertNoFailure();
		job.markProcessing();
		assertNoFailure();
		job.complete(new ClothesAnalysisResDto("상의", "셔츠", "면", "흰색", "세탁", "주의"));
		assertNoFailure();
		assertThat(service.getStatus(1L, 7L).result()).isNotNull();
	}

	private void assertNoFailure() {
		ClothesAnalysisJobStatusResDto response = service.getStatus(1L, 7L);
		assertThat(response.errorCode()).isNull();
		assertThat(response.errorMessage()).isNull();
	}

	private void assertFailure(ClothesErrorCode errorCode) {
		assertThat(job.getStatus()).isEqualTo(ClothesAnalysisJobStatus.FAILED);
		assertThat(job.getErrorMessage()).isEqualTo(errorCode.getMessage());
		ClothesAnalysisJobStatusResDto response = service.getStatus(1L, 7L);
		assertThat(response.errorCode()).isEqualTo(errorCode.getCode());
		assertThat(response.errorMessage()).isEqualTo(errorCode.getMessage());
		assertThat(response.result()).isNull();
	}
}

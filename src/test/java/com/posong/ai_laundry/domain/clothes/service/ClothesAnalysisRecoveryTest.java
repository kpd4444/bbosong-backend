package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.entity.ClothesAnalysisJob;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:analysis-recovery;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"analysis-job.recovery-interval=24h"
})
class ClothesAnalysisRecoveryTest {
	@Autowired ClothesAnalysisJobRepository jobs;
	@Autowired TransactionTemplate transactions;
	@Autowired JdbcTemplate jdbc;
	@MockitoBean ClothesAnalysisService analyzer;
	private ClothesAnalysisAsyncService first;
	private ClothesAnalysisAsyncService second;
	private static final ClothesAnalysisResDto RESULT = new ClothesAnalysisResDto("상의", "셔츠", "면", "흰색", "세탁", "주의");

	@BeforeEach
	void setUp() {
		jobs.deleteAll();
		first = new ClothesAnalysisAsyncService(jobs, analyzer, Runnable::run, transactions);
		second = new ClothesAnalysisAsyncService(jobs, analyzer, Runnable::run, transactions);
	}

	@Test
	void competingWorkersOnlyCallAiOnceAndDoNotHoldLockDuringAi() throws Exception {
		Long id = pending();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenAnswer(invocation -> {
			started.countDown();
			assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
			return RESULT;
		});
		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			Future<?> running = threads.submit(() -> first.process(id));
			try {
				assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
				threads.submit(() -> second.process(id)).get(3, TimeUnit.SECONDS);
				assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.PROCESSING);
			} finally { release.countDown(); }
			running.get(5, TimeUnit.SECONDS);
		}
		first.process(id);
		second.process(id);
		verify(analyzer, times(1)).analyze(any(byte[].class), any(MimeType.class));
		assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.SUCCESS);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void expiredJobsRejectLateSuccessAndFailure(boolean throwFailure) throws Exception {
		Long id = pending();
		AtomicReference<LocalDateTime> expiredAt = new AtomicReference<>();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenAnswer(invocation -> {
			started.countDown();
			assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
			if (throwFailure) throw new IllegalStateException("late failure");
			return RESULT;
		});
		try (ExecutorService thread = Executors.newSingleThreadExecutor()) {
			Future<?> running = thread.submit(() -> first.process(id));
			try {
				assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
				jdbc.update("update clothes_analysis_job set updated_at=? where analysis_job_id=?",
						LocalDateTime.now().minusMinutes(11), id);
				second.recoverUnfinishedJobs();
				assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.FAILED);
				expiredAt.set(jobs.findById(id).orElseThrow().getCompletedAt());
			} finally { release.countDown(); }
			running.get(5, TimeUnit.SECONDS);
		}
		first.process(id);
		var after = jobs.findById(id).orElseThrow();
		assertThat(after.getStatus()).isEqualTo(ClothesAnalysisJobStatus.FAILED);
		assertThat(after.toResult()).isNull();
		assertThat(after.getCompletedAt()).isEqualTo(expiredAt.get());
		verify(analyzer, times(1)).analyze(any(byte[].class), any(MimeType.class));
	}

	@Test
	void recoveryDoesNotFailRecentProcessingAndRestartsPending() {
		Long recent = pending();
		jdbc.update("update clothes_analysis_job set status='PROCESSING' where analysis_job_id=?", recent);
		Long waiting = pending();
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenReturn(RESULT);
		second.recoverUnfinishedJobs();
		assertThat(state(recent)).isEqualTo(ClothesAnalysisJobStatus.PROCESSING);
		assertThat(state(waiting)).isEqualTo(ClothesAnalysisJobStatus.SUCCESS);
		verify(analyzer, times(1)).analyze(any(byte[].class), any(MimeType.class));
	}

	@Test
	void repeatedRecoveryDoesNotFillLocalQueueWithDuplicates() {
		Long id = pending();
		List<Runnable> queue = new ArrayList<>();
		var queued = new ClothesAnalysisAsyncService(jobs, analyzer, queue::add, transactions);
		queued.recoverUnfinishedJobs();
		queued.recoverUnfinishedJobs();
		assertThat(queue).hasSize(1);
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenReturn(RESULT);
		queue.getFirst().run();
		assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.SUCCESS);
	}

	@Test
	void rejectedRecoverySurvivesNewServiceInstance() {
		Long id = pending();
		var saturated = new ClothesAnalysisAsyncService(jobs, analyzer,
				command -> { throw new RejectedExecutionException("full"); }, transactions);
		saturated.recoverUnfinishedJobs();
		assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.PENDING);
		verifyNoInteractions(analyzer);
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenReturn(RESULT);
		second.recoverUnfinishedJobs();
		assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.SUCCESS);
	}

	@Test
	void missingAndFailedJobsDoNotInvokeAi() {
		Long id = pending();
		jdbc.update("update clothes_analysis_job set status='FAILED' where analysis_job_id=?", id);
		first.process(id);
		first.process(Long.MAX_VALUE);
		verifyNoInteractions(analyzer);
	}

	@Test
	void simultaneousClaimFromTwoServicesOnlyExecutesOnce() throws Exception {
		Long id = pending();
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenReturn(RESULT);
		CyclicBarrier gate = new CyclicBarrier(2);
		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			Future<?> a = threads.submit(() -> { gate.await(5, TimeUnit.SECONDS); first.process(id); return null; });
			Future<?> b = threads.submit(() -> { gate.await(5, TimeUnit.SECONDS); second.process(id); return null; });
			a.get(10, TimeUnit.SECONDS);
			b.get(10, TimeUnit.SECONDS);
		}
		verify(analyzer, times(1)).analyze(any(byte[].class), any(MimeType.class));
		assertThat(state(id)).isEqualTo(ClothesAnalysisJobStatus.SUCCESS);
	}

	@Test
	void newlySubmittedJobRejectedByQueueFailsWithoutRetry() throws Exception {
		var saturated = new ClothesAnalysisAsyncService(jobs, analyzer,
				command -> { throw new RejectedExecutionException("full"); }, transactions);
		var image = com.posong.ai_laundry.global.file.ImageFixtures.jpegWithExif(1);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> saturated.submit(1L, image))
				.isInstanceOf(com.posong.ai_laundry.global.error.exception.GeneralException.class);
		assertThat(jobs.findAll()).singleElement().satisfies(job -> {
			assertThat(job.getStatus()).isEqualTo(ClothesAnalysisJobStatus.FAILED);
			assertThat(job.getErrorMessage()).contains("queue is full");
		});
		second.recoverUnfinishedJobs();
		verifyNoInteractions(analyzer);
	}

	@Test
	void queueRejectionCannotOverwriteJobCompletedByAnotherService() throws Exception {
		when(analyzer.analyze(any(byte[].class), any(MimeType.class))).thenReturn(RESULT);
		var racing = new ClothesAnalysisAsyncService(jobs, analyzer, command -> {
			second.process(jobs.findAll().getFirst().getAnalysisJobId());
			throw new RejectedExecutionException("local queue full after remote completion");
		}, transactions);
		racing.submit(1L, com.posong.ai_laundry.global.file.ImageFixtures.jpegWithExif(1));
		assertThat(jobs.findAll()).singleElement().satisfies(job ->
				assertThat(job.getStatus()).isEqualTo(ClothesAnalysisJobStatus.SUCCESS));
	}

	@Test
	void processingDeadlineMustExceedApiTimeout() {
		org.springframework.test.util.ReflectionTestUtils.setField(first, "processingTimeout", java.time.Duration.ofSeconds(60));
		org.assertj.core.api.Assertions.assertThatThrownBy(first::validateTimeouts)
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Long pending() {
		return jobs.saveAndFlush(ClothesAnalysisJob.create(1L, "image/png", new byte[]{1})).getAnalysisJobId();
	}

	private ClothesAnalysisJobStatus state(Long id) {
		return jobs.findById(id).orElseThrow().getStatus();
	}
}

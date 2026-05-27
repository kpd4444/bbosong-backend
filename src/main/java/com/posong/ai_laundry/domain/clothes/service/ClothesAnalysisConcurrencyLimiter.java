package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class ClothesAnalysisConcurrencyLimiter {

	private final Semaphore semaphore;
	private final Duration queueTimeout;
	private final MeterRegistry meterRegistry;

	public ClothesAnalysisConcurrencyLimiter(
			@Value("${clothes.analysis.concurrency.max-active:5}") int maxActive,
			@Value("${clothes.analysis.concurrency.queue-timeout:60s}") Duration queueTimeout,
			MeterRegistry meterRegistry
	) {
		if (maxActive <= 0) {
			throw new IllegalArgumentException("의류 분석 동시 실행 제한은 1 이상이어야 합니다.");
		}
		if (queueTimeout == null || queueTimeout.isNegative()) {
			throw new IllegalArgumentException("의류 분석 큐 대기 시간은 0 이상이어야 합니다.");
		}
		this.semaphore = new Semaphore(maxActive, true);
		this.queueTimeout = queueTimeout;
		this.meterRegistry = meterRegistry;
	}

	public <T> T execute(Supplier<T> supplier) {
		Timer.Sample queueTimer = Timer.start(meterRegistry);
		boolean acquired = false;
		try {
			acquired = semaphore.tryAcquire(queueTimeout.toMillis(), TimeUnit.MILLISECONDS);
			recordQueueWait(queueTimer, acquired ? "acquired" : "timeout");
			if (!acquired) {
				throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_TIMEOUT);
			}
			return supplier.get();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			recordQueueWait(queueTimer, "interrupted");
			throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_TIMEOUT);
		} finally {
			if (acquired) {
				semaphore.release();
			}
		}
	}

	private void recordQueueWait(Timer.Sample sample, String outcome) {
		sample.stop(Timer.builder("clothes.analysis.queue.wait")
				.description("Queue wait time before clothes image analysis starts")
				.tag("outcome", outcome)
				.register(meterRegistry));
	}
}

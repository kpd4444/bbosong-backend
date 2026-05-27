package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClothesAnalysisConcurrencyLimiterTest {

	@Test
	void queuesRequestUntilPermitIsAvailable() throws InterruptedException {
		ClothesAnalysisConcurrencyLimiter limiter = new ClothesAnalysisConcurrencyLimiter(
				1,
				Duration.ofSeconds(1),
				new SimpleMeterRegistry()
		);
		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		AtomicReference<String> secondResult = new AtomicReference<>();

		Thread first = new Thread(() -> limiter.execute(() -> {
			firstStarted.countDown();
			await(releaseFirst);
			return "first";
		}));
		first.start();
		assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

		Thread second = new Thread(() -> secondResult.set(limiter.execute(() -> "second")));
		second.start();
		Thread.sleep(100);

		assertThat(secondResult.get()).isNull();

		releaseFirst.countDown();
		first.join(1000);
		second.join(1000);

		assertThat(secondResult.get()).isEqualTo("second");
	}

	@Test
	void failsWhenQueueWaitTimesOut() throws InterruptedException {
		ClothesAnalysisConcurrencyLimiter limiter = new ClothesAnalysisConcurrencyLimiter(
				1,
				Duration.ofMillis(50),
				new SimpleMeterRegistry()
		);
		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);

		Thread first = new Thread(() -> limiter.execute(() -> {
			firstStarted.countDown();
			await(releaseFirst);
			return "first";
		}));
		first.start();
		assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> limiter.execute(() -> "second"))
				.isInstanceOf(GeneralException.class)
				.extracting("errorCode")
				.isEqualTo(ClothesErrorCode.CLOTHES_ANALYSIS_QUEUE_TIMEOUT);

		releaseFirst.countDown();
		first.join(1000);
	}

	@Test
	void rejectsInvalidSettings() {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

		assertThatThrownBy(() -> new ClothesAnalysisConcurrencyLimiter(0, Duration.ofSeconds(1), meterRegistry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("의류 분석 동시 실행 제한은 1 이상이어야 합니다.");
		assertThatThrownBy(() -> new ClothesAnalysisConcurrencyLimiter(1, Duration.ofMillis(-1), meterRegistry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("의류 분석 큐 대기 시간은 0 이상이어야 합니다.");
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}

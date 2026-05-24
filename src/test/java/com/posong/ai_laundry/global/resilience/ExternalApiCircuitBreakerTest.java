package com.posong.ai_laundry.global.resilience;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalApiCircuitBreakerTest {

	@Test
	void opensCircuitAfterFailureThreshold() {
		MutableClock clock = new MutableClock();
		ExternalApiCircuitBreaker circuitBreaker = new ExternalApiCircuitBreaker(2, Duration.ofSeconds(30), clock);

		assertThatThrownBy(() -> circuitBreaker.execute("openai", this::fail))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> circuitBreaker.execute("openai", this::fail))
				.isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> circuitBreaker.execute("openai", () -> "ok"))
				.isInstanceOf(ExternalApiCircuitOpenException.class)
				.hasMessageContaining("circuit=openai")
				.hasMessageContaining("remaining=");
	}

	@Test
	void closesCircuitAfterOpenDurationAndSuccessfulHalfOpenCall() {
		MutableClock clock = new MutableClock();
		ExternalApiCircuitBreaker circuitBreaker = new ExternalApiCircuitBreaker(1, Duration.ofSeconds(30), clock);

		assertThatThrownBy(() -> circuitBreaker.execute("kma-weather", this::fail))
				.isInstanceOf(IllegalStateException.class);

		clock.advance(Duration.ofSeconds(31));

		assertThat(circuitBreaker.execute("kma-weather", () -> "ok")).isEqualTo("ok");
		assertThat(circuitBreaker.execute("kma-weather", () -> "ok-again")).isEqualTo("ok-again");
	}

	@Test
	void countsTimeoutAsFailure() {
		MutableClock clock = new MutableClock();
		ExternalApiCircuitBreaker circuitBreaker = new ExternalApiCircuitBreaker(1, Duration.ofSeconds(30), clock);

		assertThatThrownBy(() -> circuitBreaker.execute("openai", Duration.ofMillis(10), () -> {
			sleep(100);
			return "ok";
		})).isInstanceOf(ExternalApiCallTimeoutException.class);

		assertThatThrownBy(() -> circuitBreaker.execute("openai", () -> "ok"))
				.isInstanceOf(ExternalApiCircuitOpenException.class);
	}

	@Test
	void countsTimeoutExecutorSaturationAsFailure() throws InterruptedException {
		MutableClock clock = new MutableClock();
		ExternalApiCircuitBreaker circuitBreaker = new ExternalApiCircuitBreaker(1, Duration.ofSeconds(30), clock, 1);
		CountDownLatch firstTaskStarted = new CountDownLatch(1);

		Thread firstCaller = new Thread(() -> circuitBreaker.execute("openai", Duration.ofSeconds(2), () -> {
			firstTaskStarted.countDown();
			sleep(1000);
			return "first";
		}));
		firstCaller.start();
		assertThat(firstTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();

		Thread secondCaller = new Thread(() -> circuitBreaker.execute("openai", Duration.ofSeconds(2), () -> {
			sleep(1000);
			return "second";
		}));
		secondCaller.start();
		sleep(100);

		assertThatThrownBy(() -> circuitBreaker.execute("openai", Duration.ofSeconds(2), () -> {
			sleep(1000);
			return "third";
		})).isInstanceOf(ExternalApiCallTimeoutException.class)
				.hasMessageContaining("외부 API 타임아웃 처리 스레드가 포화 상태입니다");

		assertThatThrownBy(() -> circuitBreaker.execute("openai", () -> "ok"))
				.isInstanceOf(ExternalApiCircuitOpenException.class);
	}

	@Test
	void rejectsInvalidSettings() {
		MutableClock clock = new MutableClock();

		assertThatThrownBy(() -> new ExternalApiCircuitBreaker(0, Duration.ofSeconds(30), clock, 8))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("실패 임계값은 1 이상이어야 합니다.");
		assertThatThrownBy(() -> new ExternalApiCircuitBreaker(1, Duration.ZERO, clock, 8))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("서킷 차단 시간은 0보다 커야 합니다.");
		assertThatThrownBy(() -> new ExternalApiCircuitBreaker(1, Duration.ofSeconds(30), null, 8))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Clock은 null일 수 없습니다.");
		assertThatThrownBy(() -> new ExternalApiCircuitBreaker(1, Duration.ofSeconds(30), clock, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("타임아웃 처리 스레드 제한은 1 이상이어야 합니다.");
	}

	private String fail() {
		throw new IllegalStateException("external api failed");
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	private static class MutableClock extends Clock {

		private Instant current = Instant.parse("2026-05-24T00:00:00Z");

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}

		private void advance(Duration duration) {
			current = current.plus(duration);
		}
	}
}

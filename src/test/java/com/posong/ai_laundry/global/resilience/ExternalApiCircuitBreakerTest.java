package com.posong.ai_laundry.global.resilience;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

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
				.isInstanceOf(ExternalApiCircuitOpenException.class);
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

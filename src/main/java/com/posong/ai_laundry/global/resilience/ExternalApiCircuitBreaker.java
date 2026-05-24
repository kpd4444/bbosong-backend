package com.posong.ai_laundry.global.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;

@Component
public class ExternalApiCircuitBreaker {

	private final int failureThreshold;
	private final Duration openDuration;
	private final Clock clock;
	private final ExecutorService timeoutExecutor;
	private final Map<String, CircuitState> states = new HashMap<>();

	@Autowired
	public ExternalApiCircuitBreaker(
			@Value("${external-api.circuit-breaker.failure-threshold:3}") int failureThreshold,
			@Value("${external-api.circuit-breaker.open-duration:30s}") Duration openDuration,
			@Value("${external-api.circuit-breaker.timeout-thread-limit:8}") int timeoutThreadLimit
	) {
		this(failureThreshold, openDuration, Clock.systemUTC(), timeoutThreadLimit);
	}

	ExternalApiCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
		this(failureThreshold, openDuration, clock, 8);
	}

	ExternalApiCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock, int timeoutThreadLimit) {
		this.failureThreshold = failureThreshold;
		this.openDuration = openDuration;
		this.clock = clock;
		this.timeoutExecutor = new ThreadPoolExecutor(
				0,
				timeoutThreadLimit,
				30L,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(timeoutThreadLimit),
				new ExternalApiThreadFactory()
		);
	}

	public <T> T execute(String circuitName, Supplier<T> supplier) {
		CircuitState state = stateOf(circuitName);
		beforeCall(state);

		try {
			T result = supplier.get();
			afterSuccess(state);
			return result;
		} catch (RuntimeException exception) {
			afterFailure(state);
			throw exception;
		}
	}

	public <T> T execute(String circuitName, Duration timeout, Supplier<T> supplier) {
		return execute(circuitName, () -> executeWithTimeout(timeout, supplier));
	}

	private <T> T executeWithTimeout(Duration timeout, Supplier<T> supplier) {
		Future<T> future = timeoutExecutor.submit(supplier::get);
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException exception) {
			future.cancel(true);
			throw new ExternalApiCallTimeoutException();
		} catch (InterruptedException exception) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new ExternalApiCallTimeoutException();
		} catch (Exception exception) {
			throw unwrapException(exception);
		}
	}

	private RuntimeException unwrapException(Exception exception) {
		Throwable cause = exception.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new IllegalStateException(exception);
	}

	@PreDestroy
	public void shutdown() {
		timeoutExecutor.shutdownNow();
	}

	private synchronized CircuitState stateOf(String circuitName) {
		return states.computeIfAbsent(circuitName, ignored -> new CircuitState());
	}

	private synchronized void beforeCall(CircuitState state) {
		if (state.status == CircuitStatus.CLOSED) {
			return;
		}

		Instant now = clock.instant();
		if (Duration.between(state.openedAt, now).compareTo(openDuration) < 0) {
			throw new ExternalApiCircuitOpenException();
		}

		if (state.status == CircuitStatus.HALF_OPEN) {
			throw new ExternalApiCircuitOpenException();
		}

		state.status = CircuitStatus.HALF_OPEN;
	}

	private synchronized void afterSuccess(CircuitState state) {
		state.failureCount = 0;
		state.status = CircuitStatus.CLOSED;
		state.openedAt = null;
	}

	private synchronized void afterFailure(CircuitState state) {
		state.failureCount++;
		if (state.status == CircuitStatus.HALF_OPEN || state.failureCount >= failureThreshold) {
			state.status = CircuitStatus.OPEN;
			state.openedAt = clock.instant();
		}
	}

	private enum CircuitStatus {
		CLOSED,
		OPEN,
		HALF_OPEN
	}

	private static class CircuitState {
		private int failureCount;
		private CircuitStatus status = CircuitStatus.CLOSED;
		private Instant openedAt;
	}

	private static class ExternalApiThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setName("external-api-timeout-" + thread.threadId());
			thread.setDaemon(true);
			return thread;
		}
	}
}

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
import java.util.concurrent.RejectedExecutionException;
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
		if (failureThreshold <= 0) {
			throw new IllegalArgumentException("실패 임계값은 1 이상이어야 합니다.");
		}
		if (openDuration == null || openDuration.isNegative() || openDuration.isZero()) {
			throw new IllegalArgumentException("서킷 차단 시간은 0보다 커야 합니다.");
		}
		if (clock == null) {
			throw new IllegalArgumentException("Clock은 null일 수 없습니다.");
		}
		if (timeoutThreadLimit <= 0) {
			throw new IllegalArgumentException("타임아웃 처리 스레드 제한은 1 이상이어야 합니다.");
		}

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
		beforeCall(circuitName, state);

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
		return execute(circuitName, () -> executeWithTimeout(circuitName, timeout, supplier));
	}

	private <T> T executeWithTimeout(String circuitName, Duration timeout, Supplier<T> supplier) {
		Future<T> future;
		try {
			future = timeoutExecutor.submit(supplier::get);
		} catch (RejectedExecutionException exception) {
			throw new ExternalApiCallTimeoutException("외부 API 타임아웃 처리 스레드가 포화 상태입니다: circuit="
					+ circuitName + ", timeout=" + timeout, exception);
		}

		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException exception) {
			future.cancel(true);
			throw new ExternalApiCallTimeoutException("External API call timed out: circuit="
					+ circuitName + ", timeout=" + timeout, exception);
		} catch (InterruptedException exception) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new ExternalApiCallTimeoutException("External API call interrupted: circuit="
					+ circuitName + ", timeout=" + timeout, exception);
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

	private synchronized void beforeCall(String circuitName, CircuitState state) {
		if (state.status == CircuitStatus.CLOSED) {
			return;
		}

		Instant now = clock.instant();
		Duration elapsed = Duration.between(state.openedAt, now);
		if (elapsed.compareTo(openDuration) < 0) {
			Duration remaining = openDuration.minus(elapsed);
			throw new ExternalApiCircuitOpenException("서킷이 열려 외부 API 호출을 차단했습니다: circuit="
					+ circuitName + ", remaining=" + remaining);
		}

		if (state.status == CircuitStatus.HALF_OPEN) {
			throw new ExternalApiCircuitOpenException("서킷 회복 확인 요청이 이미 진행 중입니다: circuit="
					+ circuitName);
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

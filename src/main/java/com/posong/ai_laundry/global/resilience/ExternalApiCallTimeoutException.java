package com.posong.ai_laundry.global.resilience;

public class ExternalApiCallTimeoutException extends RuntimeException {

	public ExternalApiCallTimeoutException() {
		super();
	}

	public ExternalApiCallTimeoutException(String message) {
		super(message);
	}

	public ExternalApiCallTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}

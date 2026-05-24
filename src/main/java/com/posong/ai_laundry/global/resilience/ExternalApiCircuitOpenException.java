package com.posong.ai_laundry.global.resilience;

public class ExternalApiCircuitOpenException extends RuntimeException {

	public ExternalApiCircuitOpenException() {
		super();
	}

	public ExternalApiCircuitOpenException(String message) {
		super(message);
	}

	public ExternalApiCircuitOpenException(String message, Throwable cause) {
		super(message, cause);
	}
}

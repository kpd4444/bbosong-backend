package com.posong.ai_laundry.global.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RefreshTokenHashProvider {

	public String hash(String refreshToken) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return toHex(hashedBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", exception);
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			stringBuilder.append(String.format("%02x", value));
		}
		return stringBuilder.toString();
	}
}

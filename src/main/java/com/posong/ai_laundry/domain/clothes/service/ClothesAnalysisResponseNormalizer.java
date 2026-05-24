package com.posong.ai_laundry.domain.clothes.service;

final class ClothesAnalysisResponseNormalizer {

	private ClothesAnalysisResponseNormalizer() {
	}

	static String normalize(String responseText) {
		if (responseText == null) {
			return null;
		}

		String normalized = stripCodeFence(responseText.trim());
		return extractJsonObject(normalized).trim();
	}

	private static String stripCodeFence(String value) {
		if (!value.startsWith("```")) {
			return value;
		}

		int firstLineEnd = value.indexOf('\n');
		if (firstLineEnd < 0) {
			return value;
		}

		String withoutOpeningFence = value.substring(firstLineEnd + 1);
		int closingFenceStart = withoutOpeningFence.lastIndexOf("```");
		if (closingFenceStart < 0) {
			return withoutOpeningFence.trim();
		}

		return withoutOpeningFence.substring(0, closingFenceStart).trim();
	}

	private static String extractJsonObject(String value) {
		int start = value.indexOf('{');
		if (start < 0) {
			return value;
		}

		int depth = 0;
		boolean inString = false;
		boolean escaped = false;

		for (int index = start; index < value.length(); index++) {
			char current = value.charAt(index);

			if (escaped) {
				escaped = false;
				continue;
			}

			if (current == '\\' && inString) {
				escaped = true;
				continue;
			}

			if (current == '"') {
				inString = !inString;
				continue;
			}

			if (inString) {
				continue;
			}

			if (current == '{') {
				depth++;
			} else if (current == '}') {
				depth--;
				if (depth == 0) {
					return value.substring(start, index + 1);
				}
			}
		}

		return value;
	}
}

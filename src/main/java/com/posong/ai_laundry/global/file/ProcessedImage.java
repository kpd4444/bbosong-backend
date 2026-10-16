package com.posong.ai_laundry.global.file;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

/** Metadata-free, oriented and resized image bytes shared by storage and AI calls. */
public record ProcessedImage(byte[] bytes, MimeType mimeType) {
	public Resource resource() {
		return new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return "image." + ("png".equals(mimeType.getSubtype()) ? "png" : "jpg");
			}
		};
	}
}

package com.posong.ai_laundry.global.file;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class ImageFileValidator {

	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	private ImageFileValidator() {
	}

	public static MimeType detectSupportedMimeType(MultipartFile file) {
		try {
			MimeType mimeType = detectByMagicNumber(file);
			validateDecodableImage(file);
			return mimeType;
		} catch (IOException exception) {
			throw new IllegalArgumentException("Invalid image file", exception);
		}
	}

	private static MimeType detectByMagicNumber(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream()) {
			byte[] header = inputStream.readNBytes(PNG_MAGIC.length);

			if (startsWith(header, PNG_MAGIC)) {
				return MimeTypeUtils.IMAGE_PNG;
			}
			if (startsWith(header, JPEG_MAGIC)) {
				return MimeTypeUtils.IMAGE_JPEG;
			}
			throw new IllegalArgumentException("Unsupported image signature");
		}
	}

	private static void validateDecodableImage(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream()) {
			BufferedImage bufferedImage = ImageIO.read(inputStream);
			if (bufferedImage == null) {
				throw new IllegalArgumentException("Undecodable image");
			}
		}
	}

	private static boolean startsWith(byte[] source, byte[] target) {
		if (source.length < target.length) {
			return false;
		}

		for (int index = 0; index < target.length; index++) {
			if (source[index] != target[index]) {
				return false;
			}
		}
		return true;
	}
}

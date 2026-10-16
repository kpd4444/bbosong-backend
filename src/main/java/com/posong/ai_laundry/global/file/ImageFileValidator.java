package com.posong.ai_laundry.global.file;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class ImageFileValidator {

	private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
	private static final int MAX_IMAGE_DIMENSION = 8192;
	private static final long MAX_IMAGE_PIXELS = 20_000_000L;
	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	private ImageFileValidator() {
	}

	public static MimeType detectSupportedMimeType(MultipartFile file) {
		try (DecodedImage decoded = decode(file)) {
			return decoded.mimeType();
		}
	}

	static DecodedImage decode(MultipartFile file) {
		validateSize(file);
		try {
			MimeType mimeType = detectByMagicNumber(file);
			return new DecodedImage(readValidatedImage(file), mimeType);
		} catch (IOException exception) {
			throw new IllegalArgumentException("Invalid image file", exception);
		}
	}

	private static void validateSize(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Empty image file");
		}
		if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
			throw new ImageFileSizeExceededException("Image file size exceeds 20MB");
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

	private static BufferedImage readValidatedImage(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream();
			 MemoryCacheImageInputStream imageInput = new MemoryCacheImageInputStream(inputStream)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
			if (!readers.hasNext()) {
				throw new IllegalArgumentException("Unsupported image reader");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(imageInput, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width <= 0 || height <= 0) {
					throw new IllegalArgumentException("Invalid image dimensions");
				}
				if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
						|| (long) width * height > MAX_IMAGE_PIXELS) {
					throw new ImageDimensionsExceededException();
				}
				BufferedImage bufferedImage = reader.read(0);
				if (bufferedImage == null) {
					throw new IllegalArgumentException("Undecodable image");
				}
				return bufferedImage;
			} finally {
				reader.dispose();
			}
		}
	}

	record DecodedImage(BufferedImage image, MimeType mimeType) implements AutoCloseable {
		@Override
		public void close() {
			image.flush();
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

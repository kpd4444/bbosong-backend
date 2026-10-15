package com.posong.ai_laundry.global.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ImageFileValidatorTest {
	@ParameterizedTest
	@CsvSource({"8193,1", "1,8193", "5000,4001", "65536,65536", "2147483647,2147483647"})
	void rejectsBeforePixelDecodeAndDisposesReader(int width, int height) throws Exception {
		ImageReader reader = mock(ImageReader.class);
		when(reader.getWidth(0)).thenReturn(width);
		when(reader.getHeight(0)).thenReturn(height);
		try (var imageIO = mockStatic(ImageIO.class)) {
			imageIO.when(() -> ImageIO.getImageReaders(any())).thenReturn(List.of(reader).iterator());
			assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(pngSignature()))
					.isInstanceOf(ImageDimensionsExceededException.class);
			verify(reader, never()).read(anyInt());
			verify(reader).dispose();
		}
	}

	@ParameterizedTest
	@CsvSource({"8192,1", "1,8192", "5000,4000"})
	void acceptsExactLimitsWithoutAllocatingLargeImages(int width, int height) throws Exception {
		ImageReader reader = mock(ImageReader.class);
		when(reader.getWidth(0)).thenReturn(width);
		when(reader.getHeight(0)).thenReturn(height);
		when(reader.read(0)).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
		try (var imageIO = mockStatic(ImageIO.class)) {
			imageIO.when(() -> ImageIO.getImageReaders(any())).thenReturn(List.of(reader).iterator());
			assertThat(ImageFileValidator.detectSupportedMimeType(pngSignature())).isEqualTo(MimeTypeUtils.IMAGE_PNG);
			verify(reader).read(0);
			verify(reader).dispose();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"png", "jpg"})
	void acceptsRealSupportedImages(String format) throws Exception {
		assertThat(ImageFileValidator.detectSupportedMimeType(new MockMultipartFile("image", imageBytes(format))))
				.isEqualTo(format.equals("png") ? MimeTypeUtils.IMAGE_PNG : MimeTypeUtils.IMAGE_JPEG);
	}

	@Test
	void realPngHeaderIsRejectedBeforeMissingPixelDataIsRead() throws Exception {
		assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(oversizedPng()))
				.isInstanceOf(ImageDimensionsExceededException.class);
	}

	@Test
	void rejectsCorruptAndUnsupportedImages() {
		assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(pngSignature()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(
				new MockMultipartFile("image", new byte[]{1, 2, 3})))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void realJpegHeaderIsRejectedBeforeDecoding() throws Exception {
		byte[] bytes = imageBytes("jpg");
		boolean rewritten = false;
		for (int i = 0; i < bytes.length - 9; i++) {
			if ((bytes[i] & 255) == 255 && (bytes[i + 1] & 255) == 192) {
				ByteBuffer.wrap(bytes).putShort(i + 7, (short) 8193);
				rewritten = true;
				break;
			}
		}
		assertThat(rewritten).isTrue();
		assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(new MockMultipartFile("image", bytes)))
				.isInstanceOf(ImageDimensionsExceededException.class);
	}

	@Test
	void closesStreamsAndDisposesReaderWhenDecodeFails() throws Exception {
		AtomicInteger closedStreams = new AtomicInteger();
		var file = new MockMultipartFile("image", pngSignature().getBytes()) {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(getBytes()) {
					@Override
					public void close() throws IOException {
						closedStreams.incrementAndGet();
						super.close();
					}
				};
			}
		};
		ImageReader reader = mock(ImageReader.class);
		when(reader.getWidth(0)).thenReturn(1);
		when(reader.getHeight(0)).thenReturn(1);
		when(reader.read(0)).thenThrow(new IOException("corrupt pixels"));
		try (var imageIO = mockStatic(ImageIO.class)) {
			imageIO.when(() -> ImageIO.getImageReaders(any())).thenReturn(List.of(reader).iterator());
			assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(file))
					.isInstanceOf(IllegalArgumentException.class);
			verify(reader).dispose();
			assertThat(closedStreams.get()).isEqualTo(2);
		}
	}

	@Test
	void retainsFileSizeLimitWithoutReadingFile() {
		var file = mock(org.springframework.web.multipart.MultipartFile.class);
		when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);
		assertThatThrownBy(() -> ImageFileValidator.detectSupportedMimeType(file))
				.isInstanceOf(ImageFileSizeExceededException.class);
		verify(file).isEmpty();
		verify(file).getSize();
		verifyNoMoreInteractions(file);
	}

	private static MockMultipartFile pngSignature() {
		return new MockMultipartFile("image", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
	}

	static MockMultipartFile oversizedPng() throws Exception {
		// Only rewrite the header of a 1x1 image. No large raster is allocated.
		byte[] bytes = imageBytes("png");
		ByteBuffer.wrap(bytes).putInt(16, 8193);
		CRC32 crc = new CRC32();
		crc.update(bytes, 12, 17);
		ByteBuffer.wrap(bytes).putInt(29, (int) crc.getValue());
		return new MockMultipartFile("image", "large.png", "image/png", bytes);
	}

	private static byte[] imageBytes(String format) throws Exception {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), format, output);
			return output.toByteArray();
		}
	}
}

package com.posong.ai_laundry.global.file;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.*;

class ImageProcessorTest {
	@ParameterizedTest
	@CsvSource({"1,RGBY", "2,GRYB", "3,YBGR", "4,BYRG", "5,RBGY", "6,BRYG", "7,YGBR", "8,GYRB"})
	void correctsAllExifOrientationsAndRemovesGps(int orientation, String expected) throws Exception {
		var original = ImageFixtures.jpegWithExif(orientation);
		var originalMetadata = ImageMetadataReader.readMetadata(original.getInputStream());
		assertThat(originalMetadata.getFirstDirectoryOfType(GpsDirectory.class)).isNotNull();
		ProcessedImage result = ImageProcessor.process(original);
		BufferedImage output = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(output.getWidth()).isEqualTo(orientation >= 5 ? 40 : 60);
		assertThat(output.getHeight()).isEqualTo(orientation >= 5 ? 60 : 40);
		for (int i = 0; i < 4; i++) {
			Color actual = new Color(output.getRGB(output.getWidth() * (i % 2 * 2 + 1) / 4,
					output.getHeight() * (i / 2 * 2 + 1) / 4));
			Color target = switch (expected.charAt(i)) {
				case 'R' -> Color.RED;
				case 'G' -> Color.GREEN;
				case 'B' -> Color.BLUE;
				default -> Color.YELLOW;
			};
			assertThat(actual.getRed()).isCloseTo(target.getRed(), within(15));
			assertThat(actual.getGreen()).isCloseTo(target.getGreen(), within(15));
			assertThat(actual.getBlue()).isCloseTo(target.getBlue(), within(15));
		}
		var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(result.bytes()));
		assertThat(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)).isNull();
		assertThat(metadata.getFirstDirectoryOfType(GpsDirectory.class)).isNull();
		assertThat(result.mimeType().toString()).isEqualTo("image/jpeg");
		assertThat(result.resource().getFilename()).isEqualTo("image.jpg");
	}

	@Test
	void resizesWithoutChangingAspectRatio() throws Exception {
		var original = new MockMultipartFile("image", ImageFixtures.encode(
				new BufferedImage(3000, 1500, BufferedImage.TYPE_INT_RGB), "jpg"));
		var result = ImageProcessor.process(original);
		var output = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(output.getWidth()).isEqualTo(2048);
		assertThat(output.getHeight()).isEqualTo(1024);
	}

	@Test
	void preservesPngAlphaAndDoesNotEnlargeSmallImages() throws Exception {
		BufferedImage image = new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 0, 0x80ff0000);
		var result = ImageProcessor.process(new MockMultipartFile("image", ImageFixtures.encode(image, "png")));
		var output = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(output.getWidth()).isEqualTo(4);
		assertThat(output.getHeight()).isEqualTo(2);
		assertThat(output.getRGB(0, 0) >>> 24).isZero();
		assertThat(output.getRGB(1, 0)).isEqualTo(0x80ff0000);
		assertThat(result.mimeType().toString()).isEqualTo("image/png");
	}

	@Test
	void rejectsInvalidExifBeforeProducingOutput() throws Exception {
		var file = ImageFixtures.jpegWithExif(9);
		assertThatThrownBy(() -> ImageProcessor.process(file)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void correctsPngExifAndRemovesOriginalMetadata() throws Exception {
		var original = ImageFixtures.pngWithExif(6);
		assertThat(ImageMetadataReader.readMetadata(original.getInputStream())
				.getFirstDirectoryOfType(GpsDirectory.class)).isNotNull();
		var result = ImageProcessor.process(original);
		var output = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(output.getWidth()).isEqualTo(40);
		assertThat(output.getHeight()).isEqualTo(60);
		var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(result.bytes()));
		assertThat(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)).isNull();
		assertThat(metadata.getFirstDirectoryOfType(GpsDirectory.class)).isNull();
	}

	@Test
	void retainsPixelLimits() throws Exception {
		var file = ImageFileValidatorTest.oversizedPng();
		assertThatThrownBy(() -> ImageProcessor.process(file)).isInstanceOf(ImageDimensionsExceededException.class);
	}
}

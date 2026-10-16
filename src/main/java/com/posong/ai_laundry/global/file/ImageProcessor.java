package com.posong.ai_laundry.global.file;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class ImageProcessor {
	private static final int MAX_OUTPUT_DIMENSION = 2048;
	private static final float JPEG_QUALITY = 0.85f;

	private ImageProcessor() {
	}

	public static ProcessedImage process(MultipartFile file) {
		// Size and pixel limits are checked before decoding or parsing metadata.
		try (ImageFileValidator.DecodedImage decoded = ImageFileValidator.decode(file)) {
			int orientation = readOrientation(file);
			boolean png = "png".equals(decoded.mimeType().getSubtype());
			BufferedImage output = render(decoded.image(), orientation, png);
			try {
				return new ProcessedImage(encode(output, png), decoded.mimeType());
			} finally {
				output.flush();
			}
		}
	}

	private static int readOrientation(MultipartFile file) {
		try (InputStream input = file.getInputStream()) {
			Metadata metadata = ImageMetadataReader.readMetadata(input);
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (directory == null) {
				return 1;
			}
			if (directory.hasErrors()) {
				throw new IllegalArgumentException("Invalid EXIF metadata");
			}
			Integer orientation = directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
			if (orientation == null) {
				return 1;
			}
			if (orientation < 1 || orientation > 8) {
				throw new IllegalArgumentException("Invalid EXIF orientation");
			}
			return orientation;
		} catch (IOException | ImageProcessingException exception) {
			throw new IllegalArgumentException("Cannot read image metadata", exception);
		}
	}

	private static BufferedImage render(BufferedImage source, int orientation, boolean png) {
		int w = source.getWidth();
		int h = source.getHeight();
		int orientedWidth = orientation >= 5 ? h : w;
		int orientedHeight = orientation >= 5 ? w : h;
		double scale = Math.min(1.0, (double) MAX_OUTPUT_DIMENSION / Math.max(orientedWidth, orientedHeight));
		int width = Math.max(1, (int) Math.round(orientedWidth * scale));
		int height = Math.max(1, (int) Math.round(orientedHeight * scale));
		BufferedImage output = new BufferedImage(width, height,
				png && source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		AffineTransform orientationTransform = switch (orientation) {
			case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
			case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
			case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
			case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
			case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
			default -> new AffineTransform();
		};
		Graphics2D graphics = output.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.scale((double) width / orientedWidth, (double) height / orientedHeight);
			graphics.drawImage(source, orientationTransform, null);
		} finally {
			graphics.dispose();
		}
		return output;
	}

	private static byte[] encode(BufferedImage image, boolean png) {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(png ? "png" : "jpeg");
		if (!writers.hasNext()) {
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		}
		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			 MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
			writer.setOutput(output);
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			if (!png) {
				parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				parameters.setCompressionQuality(JPEG_QUALITY);
			}
			// Deliberately omit all original metadata, including EXIF/GPS and thumbnails.
			writer.write(null, new IIOImage(image, null, null), parameters);
			output.flush();
			return bytes.toByteArray();
		} catch (IOException exception) {
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		} finally {
			writer.dispose();
		}
	}
}

package com.posong.ai_laundry.global.file;

import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;

public final class ImageFixtures {
	private ImageFixtures() {}

	public static MockMultipartFile jpegWithExif(int orientation) throws Exception {
		BufferedImage image = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
			for (int i = 0; i < colors.length; i++) {
				graphics.setColor(colors[i]);
				graphics.fillRect(i % 2 * 30, i / 2 * 20, 30, 20);
			}
		} finally { graphics.dispose(); }
		byte[] jpeg = encode(image, "jpg");
		// Little-endian TIFF: orientation and GPS pointer in IFD0; GPS latitude reference in GPS IFD.
		ByteBuffer tiff = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN);
		tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
		tiff.putShort((short) 2);
		tiff.putShort((short) 0x112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
		tiff.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(38);
		tiff.putInt(0);
		tiff.putShort((short) 1);
		tiff.putShort((short) 1).putShort((short) 2).putInt(2).put((byte) 'N').put((byte) 0).putShort((short) 0);
		tiff.putInt(0);
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream output = new DataOutputStream(bytes)) {
			output.write(jpeg, 0, 2);
			output.writeShort(0xffe1);
			output.writeShort(2 + 6 + tiff.array().length);
			output.write(new byte[]{'E', 'x', 'i', 'f', 0, 0});
			output.write(tiff.array());
			output.write(jpeg, 2, jpeg.length - 2);
			return new MockMultipartFile("image", "private.jpg", "image/jpeg", bytes.toByteArray());
		}
	}

	public static MockMultipartFile pngWithExif(int orientation) throws Exception {
		byte[] exif = Arrays.copyOfRange(jpegWithExif(orientation).getBytes(), 12, 68);
		byte[] png = encode(new BufferedImage(60, 40, BufferedImage.TYPE_INT_ARGB), "png");
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream output = new DataOutputStream(bytes)) {
			output.write(png, 0, 33);
			output.writeInt(exif.length);
			byte[] type = {'e', 'X', 'I', 'f'};
			output.write(type);
			output.write(exif);
			CRC32 crc = new CRC32();
			crc.update(type);
			crc.update(exif);
			output.writeInt((int) crc.getValue());
			output.write(png, 33, png.length - 33);
			return new MockMultipartFile("image", "private.png", "image/png", bytes.toByteArray());
		}
	}

	public static byte[] encode(BufferedImage image, String format) throws Exception {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			ImageIO.write(image, format, bytes);
			return bytes.toByteArray();
		} finally { image.flush(); }
	}
}

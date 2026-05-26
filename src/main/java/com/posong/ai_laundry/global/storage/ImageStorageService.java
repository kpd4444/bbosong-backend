package com.posong.ai_laundry.global.storage;

import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

	private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

	private final S3Client s3Client;
	private final AwsStorageProperties awsStorageProperties;

	public String uploadClothesImage(MultipartFile image, MimeType mimeType) {
		return upload(image, mimeType, "clothes");
	}

	public String uploadChatImage(MultipartFile image, MimeType mimeType) {
		return upload(image, mimeType, "chat");
	}

	private String upload(MultipartFile image, MimeType mimeType, String prefix) {
		String key = buildImageKey(prefix, mimeType);
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(resolveBucket())
					.key(key)
					.contentType(mimeType.toString())
					.contentLength(image.getSize())
					.build();

			s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
			return key;
		} catch (IOException exception) {
			log.warn("Failed to read image before S3 upload", exception);
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException exception) {
			log.warn("Failed to upload image to S3. key={}", key, exception);
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private String buildImageKey(String prefix, MimeType mimeType) {
		LocalDate today = LocalDate.now();
		String extension = resolveExtension(mimeType);
		return "%s/%s/%s/%s.%s".formatted(
				prefix,
				today.format(YEAR_FORMATTER),
				today.format(MONTH_FORMATTER),
				UUID.randomUUID(),
				extension
		);
	}

	private String resolveBucket() {
		if (awsStorageProperties.s3() == null
				|| awsStorageProperties.s3().bucket() == null
				|| awsStorageProperties.s3().bucket().isBlank()) {
			throw new GeneralException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
		}
		return awsStorageProperties.s3().bucket();
	}

	private String resolveExtension(MimeType mimeType) {
		if ("png".equals(mimeType.getSubtype())) {
			return "png";
		}
		return "jpg";
	}
}

package com.posong.ai_laundry.global.file;

import com.posong.ai_laundry.global.storage.AwsStorageProperties;
import com.posong.ai_laundry.global.storage.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProcessedImageStorageTest {
	@Test
	void s3HeadersExtensionAndBodyMatchProcessedImage() throws Exception {
		S3Client s3 = mock(S3Client.class);
		var service = new ImageStorageService(s3,
				new AwsStorageProperties("test", new AwsStorageProperties.S3("test-bucket"), null));
		var image = ImageProcessor.process(ImageFixtures.jpegWithExif(6));
		String key = service.uploadClothesImage(image);
		var request = ArgumentCaptor.forClass(PutObjectRequest.class);
		var body = ArgumentCaptor.forClass(RequestBody.class);
		verify(s3).putObject(request.capture(), body.capture());
		assertThat(key).startsWith("clothes/").endsWith(".jpg");
		assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
		assertThat(request.getValue().contentLength()).isEqualTo(image.bytes().length);
		try (var input = body.getValue().contentStreamProvider().newStream()) {
			assertThat(input.readAllBytes()).isEqualTo(image.bytes());
		}
	}
}

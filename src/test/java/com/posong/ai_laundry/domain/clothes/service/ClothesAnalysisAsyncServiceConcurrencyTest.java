package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.constant.ClothesAnalysisJobStatus;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisJobResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.repository.ClothesAnalysisJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:clothes-analysis-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"external-api.openai.analysis-executor.thread-limit=10",
		"external-api.openai.analysis-executor.queue-capacity=50"
})
class ClothesAnalysisAsyncServiceConcurrencyTest {

	private static final byte[] PNG_BYTES = createPngBytes();

	@Autowired
	private ClothesAnalysisAsyncService clothesAnalysisAsyncService;

	@Autowired
	private ClothesAnalysisJobRepository clothesAnalysisJobRepository;

	@MockitoBean
	private ClothesAnalysisService clothesAnalysisService;

	@Test
	void processesTenJobsConcurrentlyAndQueuesTheRest() throws Exception {
		int requestCount = 15;
		CountDownLatch tenJobsStarted = new CountDownLatch(10);
		CountDownLatch releaseJobs = new CountDownLatch(1);
		AtomicInteger runningJobs = new AtomicInteger();
		AtomicInteger maxRunningJobs = new AtomicInteger();

		when(clothesAnalysisService.analyze(any(byte[].class), any(MimeType.class)))
				.thenAnswer(invocation -> {
					int running = runningJobs.incrementAndGet();
					maxRunningJobs.accumulateAndGet(running, Math::max);
					tenJobsStarted.countDown();
					releaseJobs.await(5, TimeUnit.SECONDS);
					runningJobs.decrementAndGet();
					return new ClothesAnalysisResDto("상의", "테스트 셔츠", "면", "흰색", "단독 세탁", "건조기 주의");
				});

		List<ClothesAnalysisJobResDto> jobs = new ArrayList<>();
		for (int index = 0; index < requestCount; index++) {
			jobs.add(clothesAnalysisAsyncService.submit(1L, image(index)));
		}

		assertThat(tenJobsStarted.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(maxRunningJobs.get()).isEqualTo(10);

		long queuedJobs = jobs.stream()
				.map(job -> clothesAnalysisJobRepository.findById(job.jobId()).orElseThrow())
				.filter(job -> job.getStatus() == ClothesAnalysisJobStatus.PENDING)
				.count();
		assertThat(queuedJobs).isEqualTo(5);

		releaseJobs.countDown();

		awaitAllSuccess(jobs);
	}

	private MultipartFile image(int index) {
		return new MockMultipartFile("image", "image-" + index + ".png", "image/png", PNG_BYTES);
	}

	private static byte[] createPngBytes() {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", outputStream);
			return outputStream.toByteArray();
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private void awaitAllSuccess(List<ClothesAnalysisJobResDto> jobs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5_000L;
		while (System.currentTimeMillis() < deadline) {
			long successCount = jobs.stream()
					.map(job -> clothesAnalysisJobRepository.findById(job.jobId()).orElseThrow())
					.filter(job -> job.getStatus() == ClothesAnalysisJobStatus.SUCCESS)
					.count();
			if (successCount == jobs.size()) {
				return;
			}
			Thread.sleep(100);
		}

		long successCount = jobs.stream()
				.map(job -> clothesAnalysisJobRepository.findById(job.jobId()).orElseThrow())
				.filter(job -> job.getStatus() == ClothesAnalysisJobStatus.SUCCESS)
				.count();
		assertThat(successCount).isEqualTo(jobs.size());
	}
}

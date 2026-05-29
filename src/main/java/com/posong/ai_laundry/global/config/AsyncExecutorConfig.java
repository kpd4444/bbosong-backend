package com.posong.ai_laundry.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncExecutorConfig {

	@Bean
	public Executor clothesAnalysisTaskExecutor(
			@Value("${external-api.openai.analysis-executor.thread-limit:10}") int threadLimit,
			@Value("${external-api.openai.analysis-executor.queue-capacity:50}") int queueCapacity
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadLimit);
		executor.setMaxPoolSize(threadLimit);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("clothes-analysis-");
		executor.initialize();
		return executor;
	}
}

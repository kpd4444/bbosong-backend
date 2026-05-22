package com.posong.ai_laundry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AiLaundryApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiLaundryApplication.class, args);
	}

}

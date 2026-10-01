package com.operations_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class OperationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OperationsServiceApplication.class, args);
	}

}

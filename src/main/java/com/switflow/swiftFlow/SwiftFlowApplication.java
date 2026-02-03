package com.switflow.swiftFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SwiftFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwiftFlowApplication.class, args);
	}
}

package io.berndruecker.experiments.cloudclient.java;

import io.camunda.zeebe.spring.client.EnableZeebeClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableZeebeClient
public class CamundaCloudParallelJobsJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CamundaCloudParallelJobsJavaApplication.class, args);
	}

	@Bean
	public RestTemplate createRestTemplate() {
		return new RestTemplate();
	}

}

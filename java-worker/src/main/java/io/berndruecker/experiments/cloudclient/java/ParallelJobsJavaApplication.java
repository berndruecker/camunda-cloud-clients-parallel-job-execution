package io.berndruecker.experiments.cloudclient.java;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableZeebeClient
public class ParallelJobsJavaApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ParallelJobsJavaApplication.class, args);
//		startProcessInstances(context.getBean(ZeebeClient.class));
	}

	public static void startProcessInstances(ZeebeClient zeebeClient) {
		System.out.println("Start process instances...");
		for (int i = 0; i < 500; i++) {
			zeebeClient.newCreateInstanceCommand().bpmnProcessId("rest").latestVersion().send()
				// using blocking command to avoid getting too much backpressure
				// (topic for an upcoming blog post :-))
			    .join();
		}
		System.out.println("...done");
	}

	@Bean
	public RestTemplate createRestTemplate() {
		return new RestTemplate();
	}

}

package io.berndruecker.experiments.cloudclient.restserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestServerWithLatencyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestServerWithLatencyApplication.class, args);
	}

}

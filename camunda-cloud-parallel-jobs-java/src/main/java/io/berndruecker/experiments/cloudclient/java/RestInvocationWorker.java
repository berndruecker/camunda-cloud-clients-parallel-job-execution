package io.berndruecker.experiments.cloudclient.java;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.function.Function;

@Component
public class RestInvocationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestInvocationWorker.class);

    private static final String PAYMENT_URL = "http://localhost:9090/";

    @Autowired
    private RestTemplate rest;

    @Autowired
    private JobCounter counter;

    @ZeebeWorker(type = "rest")
    public void blockingRestCall(final JobClient client, final ActivatedJob job) {
        counter.init();
        LOGGER.info("Invoke REST call...");
        String response = rest.getForObject( //
                PAYMENT_URL,
                String.class);
        LOGGER.info("...finished. Complete Job...");
        client.newCompleteCommand(job.getKey()).send()
                .join();
        counter.inc();
    }

//    @ZeebeWorker(type = "rest")
    public void nonBlockingRestCall(final JobClient client, final ActivatedJob job) {
        counter.init();
        LOGGER.info("Invoke REST call...");
        Flux<String> paymentResponseFlux = WebClient.create()
                .get()
                .uri(PAYMENT_URL)
                .retrieve()
                .bodyToFlux(String.class);

        paymentResponseFlux.subscribe(
            response -> {
                LOGGER.info("...finished. Complete Job...");
                client.newCompleteCommand(job.getKey()).send()
                    .thenApply(jobResponse -> { counter.inc(); return jobResponse;})
                    .exceptionally(t -> {throw new RuntimeException("Could not complete job: " + t.getMessage(), t);});
            },
            exception -> {
                LOGGER.info("...REST invocation problem: " + exception.getMessage());
                client.newFailCommand(job.getKey())
                       .retries(1)
                       .errorMessage("Could not invoke REST API: " + exception.getMessage()).send()
                      .exceptionally(t -> {throw new RuntimeException("Could not fail job: " + t.getMessage(), t);});
            });


    }

}

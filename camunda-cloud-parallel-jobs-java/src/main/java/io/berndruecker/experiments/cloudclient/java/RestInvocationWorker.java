package io.berndruecker.experiments.cloudclient.java;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.function.Function;

@Component
public class RestInvocationWorker {

    private static final String PAYMENT_URL = "http://localhost:9090/";

    @Autowired
    private RestTemplate rest;

    @ZeebeWorker(type = "rest")
    public void blockingRestCall(final JobClient client, final ActivatedJob job) {
        final String message_content = (String) job.getVariablesAsMap().get("message_content");

        System.out.println("Invoce REST call...");
        String response = rest.getForObject( //
                PAYMENT_URL,
                String.class);
        System.out.println("...finished. Complete Job...");
        client.newCompleteCommand(job.getKey()).send()
                .join();
        System.out.println("...completed.");
    }

    @ZeebeWorker(type = "restX")
    public void nonBlockingRestCall(final JobClient client, final ActivatedJob job) {
        Flux<String> paymentResponseFlux = WebClient.create()
                .get()
                .uri(PAYMENT_URL)
                .retrieve()
                .bodyToFlux(String.class);

        System.out.println("Invoce REST call...");
        paymentResponseFlux.subscribe(response -> {
            System.out.println("...finished. Complete Job...");
            client.newCompleteCommand(job.getKey()).send()
                .thenApply(jobResponse -> {System.out.println("...completed."); return jobResponse;})
                .exceptionally(t -> {throw new RuntimeException("Could not complete job: " + t.getMessage(), t);});
        });


    }

}

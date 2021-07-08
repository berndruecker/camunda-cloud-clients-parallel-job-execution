package io.berndruecker.experiments.cloudclient.restserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestHandler.class);

    public Mono<ServerResponse> restServiceWithLatency(ServerRequest request) {
        LOGGER.info("Got a request...");
        long start = System.currentTimeMillis();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException("Wired things happened while thread should sleep a bit: " + e.getMessage(), e);
        }
        long end = System.currentTimeMillis();
        LOGGER.info("...took " + (end - start) + " ms");
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromValue("{}"));
    }
}
package io.berndruecker.experiments.cloudclient.restserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RestRouter {

    @Bean
    public RouterFunction<ServerResponse> route(RestHandler handler) {

        return RouterFunctions
                .route(RequestPredicates.GET("/"), handler::restServiceWithLatency);
    }
}
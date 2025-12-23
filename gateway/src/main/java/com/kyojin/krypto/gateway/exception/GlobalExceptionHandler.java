package com.kyojin.krypto.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  WebProperties webProperties,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer configurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request,
                ErrorAttributeOptions.defaults());

        log.error("Error handling request {}: {}", request.path(), error.getMessage(), error);

        HttpStatus status = determineStatus(error, errorPropertiesMap);

        errorPropertiesMap.put("status", status.value());
        errorPropertiesMap.put("error", status.getReasonPhrase());

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            errorPropertiesMap.put("message", "Too many requests. Please slow down.");
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            errorPropertiesMap.put("message", "The downstream service is unreachable. Please try again later.");
        } else {
            if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                errorPropertiesMap.put("message", "An unexpected error occurred.");
            }
        }

        errorPropertiesMap.remove("requestId");
        errorPropertiesMap.remove("timestamp");

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }

    private HttpStatus determineStatus(Throwable error, Map<String, Object> errorPropertiesMap) {
        if (error instanceof ConnectException || error.getCause() instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        if (errorPropertiesMap.containsKey("status")) {
            int statusValue = (int) errorPropertiesMap.get("status");
            return HttpStatus.valueOf(statusValue);
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
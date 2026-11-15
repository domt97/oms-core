package com.dotran.oms.core.rest.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds {@link RequestIdClientInterceptor} to every {@code RestClient.Builder} that Spring Boot hands out, in services that
 * component-scan {@code com.dotran.oms} (there is no auto-configuration; new services need the same {@code scanBasePackages}).
 * Inject the builder; {@code RestClient.create()} / {@code RestClient.builder()} built by hand do not get it). Internal clients use
 * the injected builder. A **third-party** client (payment provider, delivery partner) must be built with
 * {@code RestClient.builder()} so it never receives our ids, or the service turns this off with
 * {@code app.http-client.propagate-request-id=false}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.http-client.propagate-request-id", havingValue = "true", matchIfMissing = true)
public class RequestIdClientConfig {

    @Bean
    public RestClientCustomizer requestIdRestClientCustomizer() {
        RequestIdClientInterceptor interceptor = new RequestIdClientInterceptor();
        return builder -> builder.requestInterceptors(interceptors -> {
            if (interceptors.stream().noneMatch(RequestIdClientInterceptor.class::isInstance)) {
                interceptors.add(interceptor);
            }
        });
    }
}

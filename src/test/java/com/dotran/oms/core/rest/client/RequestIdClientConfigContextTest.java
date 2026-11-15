package com.dotran.oms.core.rest.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerList;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The wiring a service gets: Boot's RestClient auto-configuration plus oms-core's customizer (component-scanned). */
@DisplayName("RequestIdClientConfig in a Spring context")
class RequestIdClientConfigContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(RequestIdClientConfig.class);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("a client from the injected RestClient.Builder sends the MDC requestId, once")
    void injectedBuilder_sendsHeader() {
        runner.run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("http://stores/x"))
                    .andExpect(headerList("X-Request-Id", contains("req-1")))
                    .andRespond(withSuccess());
            MDC.put("requestId", "req-1");

            builder.build().get().uri("http://stores/x").retrieve().toBodilessEntity();

            server.verify();
        });
    }

    @Test
    @DisplayName("app.http-client.propagate-request-id=false → no customizer, no header")
    void switchedOff_noHeader() {
        runner.withPropertyValues("app.http-client.propagate-request-id=false").run(context -> {
            assertThat(context).doesNotHaveBean("requestIdRestClientCustomizer");
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("http://stores/x")).andExpect(headerDoesNotExist("X-Request-Id"))
                    .andRespond(withSuccess());
            MDC.put("requestId", "req-1");

            builder.build().get().uri("http://stores/x").retrieve().toBodilessEntity();

            server.verify();
        });
    }

    @Test
    @DisplayName("applying the customizer twice to one builder still registers the interceptor once")
    void appliedTwice_once() {
        runner.run(context -> {
            var customizer = context.getBean("requestIdRestClientCustomizer",
                    org.springframework.boot.web.client.RestClientCustomizer.class);
            RestClient.Builder builder = RestClient.builder();
            customizer.customize(builder);
            customizer.customize(builder);
            int[] count = {0};
            builder.requestInterceptors(list -> count[0] = (int) list.stream()
                    .filter(RequestIdClientInterceptor.class::isInstance).count());
            assertThat(count[0]).isEqualTo(1);
        });
    }
}

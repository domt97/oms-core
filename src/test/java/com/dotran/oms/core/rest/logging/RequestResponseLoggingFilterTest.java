package com.dotran.oms.core.rest.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseLoggingFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private RequestResponseLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestResponseLoggingFilter(new RequestLoggingProperties());
        ReflectionTestUtils.invokeMethod(filter, "init");
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private String incomingLine(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("Incoming Request"))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("CR/LF in query parameter names and values, and in header values, are escaped")
    void escapesInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/store/x");
        request.addParameter("q", "a\r\nINFO fake");
        request.addParameter("b\r\nWARN fake", "1");
        request.addHeader("X-Note", "x\ny");

        String line = incomingLine(request);

        assertThat(line).doesNotContain("\n").doesNotContain("\r");
        assertThat(line).contains("q=a\\u000d\\u000aINFO fake");
        assertThat(line).contains("b\\u000d\\u000aWARN fake=1");
        assertThat(line).contains("x\\u000ay");
    }

    @Test
    @DisplayName("sensitive query params and headers are still redacted")
    void stillRedacts() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addParameter("token", "secret");
        request.addHeader("Authorization", "Bearer secret");

        String line = incomingLine(request);

        assertThat(line).doesNotContain("secret");
        assertThat(line).contains("token=[REDACTED]");
    }
}

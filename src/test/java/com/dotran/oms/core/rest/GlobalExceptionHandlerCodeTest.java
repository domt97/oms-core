package com.dotran.oms.core.rest;

import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler error code")
class GlobalExceptionHandlerCodeTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/cart/x");
    /** Spring Boot's mapper is built from this builder (no inclusion setting anywhere: nulls are written). */
    private final ObjectMapper boot = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("a coded business error → 422 with the code in the body")
    void coded() {
        ErrorResponse body = handler.businessException(BusinessException.withCode("CART_LOCKED", "Checkout running"), request);

        assertThat(body.getStatus()).isEqualTo(422);
        assertThat(body.getCode()).isEqualTo("CART_LOCKED");
        assertThat(body.getError()).isEqualTo("Checkout running");
        assertThat(body.getPath()).isEqualTo("/v1/cart/x");
        assertThat(boot.valueToTree(body).get("code").asText()).isEqualTo("CART_LOCKED");
    }

    @Test
    @DisplayName("an uncoded error keeps today's JSON: no code key at all (other null fields are still written)")
    void uncoded_shapeUnchanged() {
        JsonNode business = boot.valueToTree(handler.businessException(new BusinessException("Rule broken"), request));
        JsonNode notFound = boot.valueToTree(handler.notFoundException(new NotFoundException("Store not found"), request));
        JsonNode invalid = boot.valueToTree(handler.validationException(new ValidationException("bad"), request));

        for (JsonNode json : new JsonNode[] {business, notFound, invalid}) {
            assertThat(json.has("code")).isFalse();
            assertThat(json.has("errors")).as("existing null fields keep being written").isTrue();
        }
        assertThat(business.get("status").asInt()).isEqualTo(422);
        assertThat(notFound.get("status").asInt()).isEqualTo(404);
        assertThat(invalid.get("status").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("errorResponse helper: a service answering a coded error with 409 keeps the code; other exceptions have none")
    void helper() {
        ErrorResponse conflict = handler.errorResponse(HttpStatus.CONFLICT,
                BusinessException.withCode("VERSION_STALE", "Reload the cart"), request);
        ErrorResponse other = handler.errorResponse(HttpStatus.CONFLICT, new IllegalStateException("x"), request);

        assertThat(conflict.getStatus()).isEqualTo(409);
        assertThat(conflict.getCode()).isEqualTo("VERSION_STALE");
        assertThat(other.getCode()).isNull();
        assertThat(other.getStatus()).isEqualTo(409);
    }
}

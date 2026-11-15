package com.dotran.oms.core.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String error;
    private List<String> errors;
    private String path;

    /** Machine-readable cause, only for errors that have one (left out of the JSON otherwise). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;
}

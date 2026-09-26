package com.dotran.oms.core.rest;

import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error("Internal Server Error")
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(value = {NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFoundException(NotFoundException ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(value = {ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validationException(ValidationException ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(value = {BusinessException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse businessException(BusinessException ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .path(request.getRequestURI())
                .build();
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        this.logError(ex);

        // 1. Extract just the error messages (not the full ObjectError.toString())
        List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        // 2. Correctly cast WebRequest to get HttpServletRequest
        HttpServletRequest httpServletRequest = ((ServletWebRequest) request).getRequest();

        ErrorResponse errorDTO = ErrorResponse.builder()
                .error("Validation failed")
                .errors(errorMessages)
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorDTO, HttpStatus.BAD_REQUEST);
    }

    protected void logError(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);
    }
}

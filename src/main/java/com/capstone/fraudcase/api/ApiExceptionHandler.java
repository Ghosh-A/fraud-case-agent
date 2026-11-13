package com.capstone.fraudcase.api;

import com.capstone.fraudcase.domain.CaseNotFoundException;
import com.capstone.fraudcase.domain.IllegalCaseTransitionException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Safe error responses — no stack traces, no sensitive payloads. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "malformed_request", "detail", "request body could not be parsed"));
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(CaseNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "detail", e.getMessage()));
    }

    @ExceptionHandler({IllegalCaseTransitionException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "invalid_state", "detail", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "invalid_input", "detail", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "internal_error", "detail", "investigation failed safely"));
    }
}

package com.skillswap.skillswap.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthValidationAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message;
        if (fieldErrors.size() == 1) {
            message = fieldErrors.values().iterator().next();
        } else if (fieldErrors.size() > 1) {
            message = "Registration validation failed: " + fieldErrors.entrySet().stream()
                    .map(error -> error.getKey() + ": " + error.getValue())
                    .collect(java.util.stream.Collectors.joining("; "));
        } else {
            message = "Registration validation failed";
        }
        return ResponseEntity.badRequest().body(Map.of(
                "message", message,
                "errors", fieldErrors
        ));
    }
}

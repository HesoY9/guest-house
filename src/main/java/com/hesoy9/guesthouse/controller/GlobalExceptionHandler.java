package com.hesoy9.guesthouse.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Applies to every REST controller in this package automatically - no per-controller try/catch needed.
// (Scoped to this package so the staff and guest web pages keep their own HTML error handling.)
@RestControllerAdvice(basePackages = "com.hesoy9.guesthouse.controller")
public class GlobalExceptionHandler {

    // Thrown for "not found" and invalid input across your services -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // Thrown for business-rule violations (double booking, disabled account) -> 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}

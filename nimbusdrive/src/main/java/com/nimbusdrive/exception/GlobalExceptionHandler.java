package com.nimbusdrive.exception;

import com.nimbusdrive.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatusException(ResponseStatusException ex) {
        String reason = ex.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "Request failed";
        }

        ApiResponse body = ApiResponse.builder()
                .success(false)
                .message(reason)
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        ApiResponse body = ApiResponse.builder()
                .success(false)
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}


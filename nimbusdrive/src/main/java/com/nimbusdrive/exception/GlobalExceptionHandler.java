package com.nimbusdrive.exception;

import com.nimbusdrive.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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
}


package com.campusconnect.backend.dto;

import java.util.Map;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Map.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
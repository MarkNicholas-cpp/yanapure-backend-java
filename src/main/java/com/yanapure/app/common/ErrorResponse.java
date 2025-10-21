package com.yanapure.app.common;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Object details,
        Instant timestamp,
        String path) {
    public static ErrorResponse of(String code, String message, Object details, String path) {
        return new ErrorResponse(code, message, details, Instant.now(), path);
    }
}

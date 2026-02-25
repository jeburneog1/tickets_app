package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ErrorResponse(
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("status")
    Integer status,
    
    @JsonProperty("error")
    String error,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("path")
    String path
) {
    public ErrorResponse(Integer status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path);
    }
}

package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record CreateEventRequest(
    @JsonProperty("name")
    String name,
    
    @JsonProperty("date")
    LocalDateTime date,
    
    @JsonProperty("location")
    String location,
    
    @JsonProperty("totalCapacity")
    Integer totalCapacity
) {
    public CreateEventRequest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Event location is required");
        }
        if (totalCapacity == null || totalCapacity <= 0) {
            throw new IllegalArgumentException("Total capacity must be positive");
        }
    }
}

package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AvailabilityResponse(
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("eventName")
    String eventName,
    
    @JsonProperty("date")
    LocalDateTime date,
    
    @JsonProperty("location")
    String location,
    
    @JsonProperty("totalCapacity")
    Integer totalCapacity,
    
    @JsonProperty("availableTickets")
    Integer availableTickets,
    
    @JsonProperty("reservedTickets")
    Integer reservedTickets,
    
    @JsonProperty("soldTickets")
    Integer soldTickets,
    
    @JsonProperty("complimentaryTickets")
    Integer complimentaryTickets,
    
    @JsonProperty("isAvailable")
    Boolean isAvailable,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {
    public AvailabilityResponse {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
    }
}

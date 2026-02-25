package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record EventResponse(
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("name")
    String name,
    
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
    
    @JsonProperty("complimentaryTickets")
    Integer complimentaryTickets,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {
}

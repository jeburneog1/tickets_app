package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssignComplimentaryTicketRequest(
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("customerId")
    String customerId,
    
    @JsonProperty("reason")
    String reason
) {
    public AssignComplimentaryTicketRequest {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
    }
}

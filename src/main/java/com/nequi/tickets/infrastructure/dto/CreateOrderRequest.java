package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateOrderRequest(
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("customerId")
    String customerId,
    
    @JsonProperty("numberOfTickets")
    Integer numberOfTickets
) {
    public CreateOrderRequest {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (numberOfTickets == null || numberOfTickets <= 0) {
            throw new IllegalArgumentException("Number of tickets must be positive");
        }
        if (numberOfTickets > 10) {
            throw new IllegalArgumentException("Maximum 10 tickets per order");
        }
    }
}

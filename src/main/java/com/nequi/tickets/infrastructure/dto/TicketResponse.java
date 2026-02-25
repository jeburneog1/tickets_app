package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nequi.tickets.domain.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
    @JsonProperty("ticketId")
    String ticketId,
    
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("status")
    TicketStatus status,
    
    @JsonProperty("customerId")
    String customerId,
    
    @JsonProperty("orderId")
    String orderId,
    
    @JsonProperty("reservedAt")
    LocalDateTime reservedAt,
    
    @JsonProperty("reservationExpiresAt")
    LocalDateTime reservationExpiresAt,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {
}

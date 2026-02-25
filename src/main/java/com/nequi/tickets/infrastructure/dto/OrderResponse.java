package com.nequi.tickets.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nequi.tickets.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    @JsonProperty("orderId")
    String orderId,
    
    @JsonProperty("eventId")
    String eventId,
    
    @JsonProperty("customerId")
    String customerId,
    
    @JsonProperty("ticketIds")
    List<String> ticketIds,
    
    @JsonProperty("status")
    OrderStatus status,
    
    @JsonProperty("totalTickets")
    Integer totalTickets,
    
    @JsonProperty("failureReason")
    String failureReason,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt,
    
    @JsonProperty("processedAt")
    LocalDateTime processedAt
) {
}

package com.nequi.tickets.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record Order(
    String orderId,
    String eventId,
    String customerId,
    List<String> ticketIds,
    OrderStatus status,
    Integer totalTickets,
    Integer retryCount,
    Integer version,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime processedAt
) {
    
    public static final int MAX_TICKETS_PER_ORDER = 10;
    
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one ticket");
        }
        if (ticketIds.size() > MAX_TICKETS_PER_ORDER) {
            throw new IllegalArgumentException(
                String.format("Order cannot have more than %d tickets", MAX_TICKETS_PER_ORDER)
            );
        }
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        if (totalTickets == null || totalTickets <= 0) {
            throw new IllegalArgumentException("Total tickets must be positive");
        }
        if (totalTickets != ticketIds.size()) {
            throw new IllegalArgumentException("Total tickets must match ticket IDs count");
        }
        if (retryCount == null || retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be null or negative");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version cannot be null or negative");
        }
        
        ticketIds = List.copyOf(ticketIds);
    }
    
    public static Order createPending(
        String orderId,
        String eventId,
        String customerId,
        List<String> ticketIds
    ) {
        return createPending(orderId, eventId, customerId, ticketIds, MAX_TICKETS_PER_ORDER);
    }
    
    public static Order createPending(
        String orderId,
        String eventId,
        String customerId,
        List<String> ticketIds,
        int maxTicketsPerOrder
    ) {
        if (ticketIds.size() > maxTicketsPerOrder) {
            throw new IllegalArgumentException(
                String.format("Cannot create order with more than %d tickets", maxTicketsPerOrder)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            orderId,
            eventId,
            customerId,
            List.copyOf(ticketIds),
            OrderStatus.PENDING,
            ticketIds.size(),
            0,
            0,
            null,
            now,
            now,
            null
        );
    }
    
    public static Order createComplimentary(
        String orderId,
        String eventId,
        String customerId,
        List<String> ticketIds
    ) {
        if (ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one ticket");
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            orderId,
            eventId,
            customerId,
            List.copyOf(ticketIds),
            OrderStatus.CONFIRMED,
            ticketIds.size(),
            0,
            0,
            null,
            now,
            now,
            now
        );
    }
    
    public Order startProcessing() {
        if (!status.canBeProcessed()) {
            throw new IllegalStateException(
                String.format("Cannot process order in %s status. Only PENDING orders can be processed.", status)
            );
        }
        
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            OrderStatus.PROCESSING,
            totalTickets,
            retryCount + 1,
            version + 1,
            failureReason,
            createdAt,
            LocalDateTime.now(),
            processedAt
        );
    }
    
    public Order confirm() {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot confirm order in %s status. Only PROCESSING orders can be confirmed.", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            OrderStatus.CONFIRMED,
            totalTickets,
            retryCount,
            version + 1,
            null,
            createdAt,
            now,
            now
        );
    }
    
    public Order fail(String reason) {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot fail order in %s status. Only PROCESSING orders can be failed.", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            OrderStatus.FAILED,
            totalTickets,
            retryCount,
            version + 1,
            reason,
            createdAt,
            now,
            now
        );
    }
    
    public Order cancel(String reason) {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException(
                String.format("Cannot cancel order in final state %s", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            OrderStatus.CANCELLED,
            totalTickets,
            retryCount,
            version + 1,
            reason,
            createdAt,
            now,
            processedAt
        );
    }
    
    public Order incrementRetry() {
        return incrementRetry(MAX_RETRY_ATTEMPTS);
    }
    
    public Order incrementRetry(int maxRetryAttempts) {
        if (retryCount >= maxRetryAttempts) {
            throw new IllegalStateException(
                String.format("Order has already been retried maximum times (%d)", maxRetryAttempts)
            );
        }
        
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            status,
            totalTickets,
            retryCount + 1,
            version + 1,
            failureReason,
            createdAt,
            LocalDateTime.now(),
            processedAt
        );
    }
    
    public boolean hasExceededMaxRetries() {
        return hasExceededMaxRetries(MAX_RETRY_ATTEMPTS);
    }
    
    public boolean hasExceededMaxRetries(int maxRetryAttempts) {
        return retryCount >= maxRetryAttempts;
    }
    
    public boolean canRetry() {
        return status == OrderStatus.PROCESSING && !hasExceededMaxRetries();
    }
    
    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }
    
    public boolean isForEvent(String eventId) {
        return this.eventId.equals(eventId);
    }
    
    @Override
    public List<String> ticketIds() {
        return List.copyOf(ticketIds);
    }
}

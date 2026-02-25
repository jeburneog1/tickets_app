package com.nequi.tickets.domain.model;

public enum OrderStatus {
    
    PENDING,
    
    PROCESSING,
    
    CONFIRMED,
    
    FAILED,
    
    CANCELLED;
    
    public boolean isFinalState() {
        return this == CONFIRMED || this == FAILED || this == CANCELLED;
    }
    
    public boolean isSuccessful() {
        return this == CONFIRMED;
    }
    
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean canBeProcessed() {
        return this == PENDING;
    }
    
    public boolean canBeCancelled() {
        return this == PENDING || this == PROCESSING;
    }
}

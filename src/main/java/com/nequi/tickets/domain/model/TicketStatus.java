package com.nequi.tickets.domain.model;

public enum TicketStatus {
    
    AVAILABLE,
    
    RESERVED,
    
    PENDING_CONFIRMATION,
    
    SOLD,
    
    COMPLIMENTARY;
    
    public boolean isFinalState() {
        return this == SOLD || this == COMPLIMENTARY;
    }
    
    public boolean isUnavailable() {
        return this != AVAILABLE;
    }
    
    public boolean isSold() {
        return this == SOLD;
    }
    
    public boolean isTemporary() {
        return this == RESERVED || this == PENDING_CONFIRMATION;
    }
}

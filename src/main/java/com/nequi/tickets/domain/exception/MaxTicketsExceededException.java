package com.nequi.tickets.domain.exception;

public class MaxTicketsExceededException extends DomainException {
    
    private final int requestedQuantity;
    private final int maxAllowed;
    
    public MaxTicketsExceededException(int requestedQuantity, int maxAllowed) {
        super(String.format(
            "Requested quantity (%d) exceeds maximum allowed tickets per order (%d)",
            requestedQuantity, maxAllowed
        ));
        this.requestedQuantity = requestedQuantity;
        this.maxAllowed = maxAllowed;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getMaxAllowed() {
        return maxAllowed;
    }
}

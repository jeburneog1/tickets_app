package com.nequi.tickets.domain.exception;

public class InsufficientTicketsException extends DomainException {
    
    private final String eventId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientTicketsException(String eventId, int requestedQuantity, int availableQuantity) {
        super(String.format(
            "Insufficient tickets for event %s. Requested: %d, Available: %d",
            eventId, requestedQuantity, availableQuantity
        ));
        this.eventId = eventId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}

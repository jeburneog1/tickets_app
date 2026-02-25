package com.nequi.tickets.domain.exception;

public class EventNotFoundException extends DomainException {
    
    private final String eventId;
    
    public EventNotFoundException(String eventId) {
        super(String.format("Event not found with ID: %s", eventId));
        this.eventId = eventId;
    }
    
    public String getEventId() {
        return eventId;
    }
}

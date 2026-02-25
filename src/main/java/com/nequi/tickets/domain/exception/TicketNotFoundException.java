package com.nequi.tickets.domain.exception;

public class TicketNotFoundException extends DomainException {
    
    private final String ticketId;
    
    public TicketNotFoundException(String ticketId) {
        super(String.format("Ticket not found with ID: %s", ticketId));
        this.ticketId = ticketId;
    }
    
    public String getTicketId() {
        return ticketId;
    }
}

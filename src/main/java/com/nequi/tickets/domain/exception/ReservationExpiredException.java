package com.nequi.tickets.domain.exception;

public class ReservationExpiredException extends DomainException {
    
    private final String ticketId;
    private final String orderId;
    
    public ReservationExpiredException(String ticketId, String orderId) {
        super(String.format(
            "Reservation expired for ticket %s in order %s. Please try again.",
            ticketId, orderId
        ));
        this.ticketId = ticketId;
        this.orderId = orderId;
    }
    
    public ReservationExpiredException(String orderId) {
        super(String.format("Reservation expired for order %s. Please try again.", orderId));
        this.ticketId = null;
        this.orderId = orderId;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public String getOrderId() {
        return orderId;
    }
}

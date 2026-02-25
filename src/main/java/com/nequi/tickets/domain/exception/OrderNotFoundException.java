package com.nequi.tickets.domain.exception;

public class OrderNotFoundException extends DomainException {
    
    private final String orderId;
    
    public OrderNotFoundException(String orderId) {
        super(String.format("Order not found with ID: %s", orderId));
        this.orderId = orderId;
    }
    
    public String getOrderId() {
        return orderId;
    }
}

package com.nequi.tickets.domain.model;

import java.time.LocalDateTime;

public record Ticket(
    String ticketId,
    String eventId,
    TicketStatus status,
    String customerId,
    String orderId,
    LocalDateTime reservedAt,
    LocalDateTime reservationExpiresAt,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public static final int RESERVATION_TIMEOUT_MINUTES = 10;
    
    public Ticket {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID cannot be null or blank");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Ticket status cannot be null");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version cannot be null or negative");
        }
        
        if (status != TicketStatus.AVAILABLE && (customerId == null || customerId.isBlank())) {
            throw new IllegalArgumentException("Customer ID is required for reserved or sold tickets");
        }
        
        if ((status == TicketStatus.RESERVED || status == TicketStatus.PENDING_CONFIRMATION) 
            && reservationExpiresAt == null) {
            throw new IllegalArgumentException("Reservation expiration time is required for reserved tickets");
        }
    }
    
    public static Ticket createAvailableTicket(String ticketId, String eventId) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.AVAILABLE,
            null,
            null,
            null,
            null,
            0,
            now,
            now
        );
    }

    public static Ticket createComplimentaryTicket(String ticketId, String eventId, String customerId) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.COMPLIMENTARY,
            customerId,
            null,
            now,
            null,
            0,
            now,
            now
        );
    }
    
    public Ticket assignAsComplimentary(String customerId) {
        if (status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException(
                String.format("Cannot assign complimentary ticket in %s status. Only AVAILABLE tickets can be assigned.", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.COMPLIMENTARY,
            customerId,
            orderId,
            now,
            null,
            version + 1,
            createdAt,
            now
        );
    }
    
    public static Ticket createReservedTicket(String ticketId, String eventId, String customerId, String orderId) {
        return createReservedTicket(ticketId, eventId, customerId, orderId, RESERVATION_TIMEOUT_MINUTES);
    }
    
    public static Ticket createReservedTicket(String ticketId, String eventId, String customerId, String orderId, int timeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(timeoutMinutes);
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.RESERVED,
            customerId,
            orderId,
            now,
            expiresAt,
            0,
            now,
            now
        );
    }
    
    public Ticket reserveTicket(String customerId, String orderId) {
        return reserveTicket(customerId, orderId, RESERVATION_TIMEOUT_MINUTES);
    }
    
    public Ticket reserveTicket(String customerId, String orderId, int timeoutMinutes) {
        if (status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException(
                String.format("Cannot reserve ticket in %s status. Only AVAILABLE tickets can be reserved.", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(timeoutMinutes);
        
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.RESERVED,
            customerId,
            orderId,
            now,
            expiresAt,
            version + 1,
            createdAt,
            now
        );
    }
    
    public Ticket startConfirmation() {
        return startConfirmation(RESERVATION_TIMEOUT_MINUTES);
    }
    
    public Ticket startConfirmation(int timeoutMinutes) {
        if (status != TicketStatus.RESERVED) {
            throw new IllegalStateException(
                String.format("Cannot start confirmation for ticket in %s status. Only RESERVED tickets can be confirmed.", status)
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiresAt = now.plusMinutes(timeoutMinutes);
        
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.PENDING_CONFIRMATION,
            customerId,
            orderId,
            reservedAt,
            newExpiresAt,
            version + 1,
            createdAt,
            now
        );
    }
    
    public Ticket confirmTicketSale() {
        if (status != TicketStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException(
                String.format("Cannot confirm sale for ticket in %s status. Only PENDING_CONFIRMATION tickets can be sold.", status)
            );
        }
        
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.SOLD,
            customerId,
            orderId,
            reservedAt,
            null,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Ticket confirmAsComplimentary() {
        if (status != TicketStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException(
                String.format("Cannot confirm complimentary for ticket in %s status. Only PENDING_CONFIRMATION tickets can be confirmed as complimentary.", status)
            );
        }
        
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.COMPLIMENTARY,
            customerId,
            orderId,
            reservedAt,
            null,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Ticket releaseTicketBack() {
        if (status.isFinalState()) {
            throw new IllegalStateException(
                String.format("Cannot release ticket in final state %s", status)
            );
        }
        
        if (status == TicketStatus.AVAILABLE) {
            throw new IllegalStateException("Ticket is already available");
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.AVAILABLE,
            null,
            null,
            null,
            null,
            version + 1,
            createdAt,
            now
        );
    }
    
    public boolean isReservationExpired() {
        if (reservationExpiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(reservationExpiresAt);
    }
    
    public boolean isTicketAvailable() {
        return status == TicketStatus.AVAILABLE;
    }
    
    public boolean belongsTo(String customerId) {
        return this.customerId != null && this.customerId.equals(customerId);
    }
    
    public boolean isPartOfOrder(String orderId) {
        return this.orderId != null && this.orderId.equals(orderId);
    }
}

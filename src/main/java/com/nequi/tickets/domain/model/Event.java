package com.nequi.tickets.domain.model;

import java.time.LocalDateTime;

public record Event(
    String eventId,
    String name,
    LocalDateTime date,
    String location,
    Integer totalCapacity,
    Integer availableTickets,
    Integer reservedTickets,
    Integer complimentaryTickets,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public Event {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be null or blank");
        }
        if (date == null) {
            throw new IllegalArgumentException("Event date cannot be null");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Event location cannot be null or blank");
        }
        if (totalCapacity == null || totalCapacity <= 0) {
            throw new IllegalArgumentException("Total capacity must be positive");
        }
        if (availableTickets == null || availableTickets < 0) {
            throw new IllegalArgumentException("Available tickets cannot be negative");
        }
        if (availableTickets > totalCapacity) {
            throw new IllegalArgumentException("Available tickets cannot exceed total capacity");
        }
        if (reservedTickets == null || reservedTickets < 0) {
            throw new IllegalArgumentException("Reserved tickets cannot be negative");
        }
        if (complimentaryTickets == null || complimentaryTickets < 0) {
            throw new IllegalArgumentException("Complimentary tickets cannot be negative");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version cannot be null or negative");
        }
    }
    
    public static Event create(
        String eventId,
        String name,
        LocalDateTime date,
        String location,
        Integer totalCapacity
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            totalCapacity,
            0,
            0,
            0,
            now,
            now
        );
    }
    
    public Event reserveTickets(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableTickets < quantity) {
            throw new IllegalArgumentException(
                String.format("Not enough tickets available. Requested: %d, Available: %d", 
                    quantity, availableTickets)
            );
        }
        
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            availableTickets - quantity,
            reservedTickets + quantity,
            complimentaryTickets,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Event releaseReservedTickets(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedTickets < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot release more than reserved. Requested: %d, Reserved: %d", 
                    quantity, reservedTickets)
            );
        }
        
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            availableTickets + quantity,
            reservedTickets - quantity,
            complimentaryTickets,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Event confirmSale(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedTickets < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot confirm more than reserved. Requested: %d, Reserved: %d", 
                    quantity, reservedTickets)
            );
        }
        
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            availableTickets,
            reservedTickets - quantity,
            complimentaryTickets,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Event confirmComplimentary(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedTickets < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot confirm more than reserved. Requested: %d, Reserved: %d", 
                    quantity, reservedTickets)
            );
        }
        
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            availableTickets,
            reservedTickets - quantity,
            complimentaryTickets + quantity,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public Event assignComplimentaryTicket(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableTickets < quantity) {
            throw new IllegalArgumentException(
                String.format("Not enough tickets available. Requested: %d, Available: %d", 
                    quantity, availableTickets)
            );
        }
        
        return new Event(
            eventId,
            name,
            date,
            location,
            totalCapacity,
            availableTickets - quantity,
            reservedTickets,
            complimentaryTickets + quantity,
            version + 1,
            createdAt,
            LocalDateTime.now()
        );
    }
    
    public int getUnavailableTickets() {
        return totalCapacity - availableTickets;
    }
    
    public int getSoldTickets() {
        // Sold tickets are tickets that were reserved and confirmed (excluding complimentary)
        return totalCapacity - availableTickets - reservedTickets - complimentaryTickets;
    }
    
    public int getComplimentaryTickets() {
        return complimentaryTickets;
    }
    
    public boolean hasAvailableTickets() {
        return availableTickets > 0;
    }
    
    public boolean canAccommodate(int quantity) {
        return availableTickets >= quantity;
    }
    
    public boolean isSoldOut() {
        return availableTickets == 0;
    }
}

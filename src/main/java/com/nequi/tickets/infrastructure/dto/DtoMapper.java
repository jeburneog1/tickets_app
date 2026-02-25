package com.nequi.tickets.infrastructure.dto;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.Ticket;

public final class DtoMapper {
    
    private DtoMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    public static Event toEntity(EventResponse response) {
        if (response == null) {
            return null;
        }
        return new Event(
            response.eventId(),
            response.name(),
            response.date(),
            response.location(),
            response.totalCapacity(),
            response.availableTickets(),
            response.reservedTickets(),
            response.complimentaryTickets() != null ? response.complimentaryTickets() : 0,
            0,
            response.createdAt(),
            response.updatedAt()
        );
    }
    
    public static EventResponse toEventResponse(Event event) {
        if (event == null) {
            return null;
        }
        return new EventResponse(
            event.eventId(),
            event.name(),
            event.date(),
            event.location(),
            event.totalCapacity(),
            event.availableTickets(),
            event.reservedTickets(),
            event.complimentaryTickets(),
            event.createdAt(),
            event.updatedAt()
        );
    }
    
    public static OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderResponse(
            order.orderId(),
            order.eventId(),
            order.customerId(),
            order.ticketIds(),
            order.status(),
            order.totalTickets(),
            order.failureReason(),
            order.createdAt(),
            order.updatedAt(),
            order.processedAt()
        );
    }
    
    public static AvailabilityResponse toAvailabilityResponse(Event event) {
        if (event == null) {
            return null;
        }
        
        int soldTickets = event.getSoldTickets();
        boolean isAvailable = event.availableTickets() > 0;
        
        return new AvailabilityResponse(
            event.eventId(),
            event.name(),
            event.date(),
            event.location(),
            event.totalCapacity(),
            event.availableTickets(),
            event.reservedTickets(),
            soldTickets,
            event.complimentaryTickets(),
            isAvailable,
            event.createdAt(),
            event.updatedAt()
        );
    }
    
    public static TicketResponse toTicketResponse(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return new TicketResponse(
            ticket.ticketId(),
            ticket.eventId(),
            ticket.status(),
            ticket.customerId(),
            ticket.orderId(),
            ticket.reservedAt(),
            ticket.reservationExpiresAt(),
            ticket.createdAt(),
            ticket.updatedAt()
        );
    }
}

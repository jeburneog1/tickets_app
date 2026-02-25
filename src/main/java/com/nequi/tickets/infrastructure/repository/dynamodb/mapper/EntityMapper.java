package com.nequi.tickets.infrastructure.repository.dynamodb.mapper;

import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.EventEntity;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.OrderEntity;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.TicketEntity;

import java.time.LocalDateTime;

public final class EntityMapper {
    
    private EntityMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static EventEntity toEventEntity(Event event) {
        if (event == null) {
            return null;
        }
        
        EventEntity entity = new EventEntity();
        entity.setEventId(event.eventId());
        entity.setName(event.name());
        entity.setDate(toIsoString(event.date()));
        entity.setLocation(event.location());
        entity.setTotalCapacity(event.totalCapacity());
        entity.setAvailableTickets(event.availableTickets());
        entity.setReservedTickets(event.reservedTickets());
        entity.setComplimentaryTickets(event.complimentaryTickets());
        entity.setVersion(event.version());
        entity.setCreatedAt(toIsoString(event.createdAt()));
        entity.setUpdatedAt(toIsoString(event.updatedAt()));
        return entity;
    }
    
    public static Event toEvent(EventEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Event(
            entity.getEventId(),
            entity.getName(),
            fromIsoString(entity.getDate()),
            entity.getLocation(),
            entity.getTotalCapacity(),
            entity.getAvailableTickets(),
            entity.getReservedTickets(),
            entity.getComplimentaryTickets() != null ? entity.getComplimentaryTickets() : 0,
            entity.getVersion(),
            fromIsoString(entity.getCreatedAt()),
            fromIsoString(entity.getUpdatedAt())
        );
    }
    
    public static TicketEntity toTicketEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(ticket.ticketId());
        entity.setEventId(ticket.eventId());
        entity.setStatus(ticket.status().name());
        entity.setCustomerId(ticket.customerId());
        entity.setOrderId(ticket.orderId());
        entity.setReservedAt(toIsoString(ticket.reservedAt()));
        entity.setReservationExpiresAt(toIsoString(ticket.reservationExpiresAt()));
        entity.setVersion(ticket.version());
        entity.setCreatedAt(toIsoString(ticket.createdAt()));
        entity.setUpdatedAt(toIsoString(ticket.updatedAt()));
        return entity;
    }
    
    public static Ticket toTicket(TicketEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Ticket(
            entity.getTicketId(),
            entity.getEventId(),
            TicketStatus.valueOf(entity.getStatus()),
            entity.getCustomerId(),
            entity.getOrderId(),
            fromIsoString(entity.getReservedAt()),
            fromIsoString(entity.getReservationExpiresAt()),
            entity.getVersion(),
            fromIsoString(entity.getCreatedAt()),
            fromIsoString(entity.getUpdatedAt())
        );
    }
    
    public static OrderEntity toOrderEntity(Order order) {
        if (order == null) {
            return null;
        }
        
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.orderId());
        entity.setEventId(order.eventId());
        entity.setCustomerId(order.customerId());
        entity.setTicketIds(order.ticketIds());
        entity.setStatus(order.status().name());
        entity.setTotalTickets(order.totalTickets());
        entity.setRetryCount(order.retryCount());
        entity.setVersion(order.version());
        entity.setFailureReason(order.failureReason());
        entity.setCreatedAt(toIsoString(order.createdAt()));
        entity.setUpdatedAt(toIsoString(order.updatedAt()));
        entity.setProcessedAt(toIsoString(order.processedAt()));
        return entity;
    }
    
    public static Order toOrder(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Order(
            entity.getOrderId(),
            entity.getEventId(),
            entity.getCustomerId(),
            entity.getTicketIds(),
            OrderStatus.valueOf(entity.getStatus()),
            entity.getTotalTickets(),
            entity.getRetryCount(),
            entity.getVersion(),
            entity.getFailureReason(),
            fromIsoString(entity.getCreatedAt()),
            fromIsoString(entity.getUpdatedAt()),
            fromIsoString(entity.getProcessedAt())
        );
    }
    
    private static String toIsoString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
    
    private static LocalDateTime fromIsoString(String isoString) {
        return isoString != null && !isoString.isBlank() ? LocalDateTime.parse(isoString) : null;
    }
}

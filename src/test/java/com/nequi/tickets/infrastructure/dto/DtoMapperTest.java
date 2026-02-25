package com.nequi.tickets.infrastructure.dto;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DtoMapper Tests")
class DtoMapperTest {
    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() {
        try {
            java.lang.reflect.Constructor<DtoMapper> constructor = DtoMapper.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        } catch (NoSuchMethodException e) {
            fail("Constructor should exist");
        }
    }
    @Test
    @DisplayName("Should map Event to EventResponse")
    void shouldMapEventToEventResponse() {
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event(
            "event-123",
            "Rock Concert",
            now.plusDays(30),
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(750),
            Integer.valueOf(100),
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        EventResponse response = DtoMapper.toEventResponse(event);
        assertNotNull(response);
        assertEquals("event-123", response.eventId());
        assertEquals("Rock Concert", response.name());
        assertEquals(now.plusDays(30), response.date());
        assertEquals("Madison Square Garden", response.location());
        assertEquals(1000, response.totalCapacity());
        assertEquals(750, response.availableTickets());
        assertEquals(100, response.reservedTickets());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
    @Test
    @DisplayName("Should return null when mapping null Event to EventResponse")
    void shouldReturnNullWhenMappingNullEventToResponse() {
        EventResponse response = DtoMapper.toEventResponse(null);
        assertNull(response);
    }
    @Test
    @DisplayName("Should map EventResponse to Event entity")
    void shouldMapEventResponseToEvent() {
        LocalDateTime now = LocalDateTime.now();
        EventResponse response = new EventResponse(
            "event-123",
            "Rock Concert",
            now.plusDays(30),
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(750),
            Integer.valueOf(100),
            Integer.valueOf(0),
            now,
            now
        );
        Event event = DtoMapper.toEntity(response);
        assertNotNull(event);
        assertEquals("event-123", event.eventId());
        assertEquals("Rock Concert", event.name());
        assertEquals(now.plusDays(30), event.date());
        assertEquals("Madison Square Garden", event.location());
        assertEquals(1000, event.totalCapacity());
        assertEquals(750, event.availableTickets());
        assertEquals(100, event.reservedTickets());
        assertEquals(0, event.version()); 
        assertEquals(now, event.createdAt());
        assertEquals(now, event.updatedAt());
    }
    @Test
    @DisplayName("Should return null when mapping null EventResponse to Event")
    void shouldReturnNullWhenMappingNullEventResponseToEvent() {
        Event event = DtoMapper.toEntity(null);
        assertNull(event);
    }
    @Test
    @DisplayName("Should map Order to OrderResponse")
    void shouldMapOrderToOrderResponse() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processedAt = now.plusMinutes(5);
        Order order = new Order(
            "order-123",
            "event-456",
            "customer-789",
            List.of("ticket-1", "ticket-2", "ticket-3"),
            OrderStatus.CONFIRMED,
            3,
            0,  
            0,  
            null,
            now,
            now,
            processedAt
        );
        OrderResponse response = DtoMapper.toOrderResponse(order);
        assertNotNull(response);
        assertEquals("order-123", response.orderId());
        assertEquals("event-456", response.eventId());
        assertEquals("customer-789", response.customerId());
        assertEquals(List.of("ticket-1", "ticket-2", "ticket-3"), response.ticketIds());
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(3, response.totalTickets());
        assertNull(response.failureReason());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
        assertEquals(processedAt, response.processedAt());
    }
    @Test
    @DisplayName("Should map Order with empty ticket list to OrderResponse")
    void shouldMapOrderWithEmptyTicketList() {
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            "order-123",
            "event-456",
            "customer-789",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.PENDING,
            5,
            0,  
            0,  
            null,
            now,
            now,
            null
        );
        OrderResponse response = DtoMapper.toOrderResponse(order);
        assertNotNull(response);
        assertEquals("order-123", response.orderId());
        assertTrue(response.ticketIds().size() == 5);
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(5, response.totalTickets());
        assertNull(response.processedAt());
    }
    @Test
    @DisplayName("Should map failed Order with failure reason to OrderResponse")
    void shouldMapFailedOrder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processedAt = now.plusMinutes(2);
        Order order = new Order(
            "order-123",
            "event-456",
            "customer-789",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5", "ticket-6", "ticket-7", "ticket-8", "ticket-9", "ticket-10"),
            OrderStatus.FAILED,
            10,
            0,  
            0,  
            "Insufficient tickets available",
            now,
            processedAt,
            processedAt
        );
        OrderResponse response = DtoMapper.toOrderResponse(order);
        assertNotNull(response);
        assertEquals("order-123", response.orderId());
        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals("Insufficient tickets available", response.failureReason());
        assertEquals(10, response.totalTickets());
        assertEquals(10, response.ticketIds().size());
        assertEquals(processedAt, response.processedAt());
    }
    @Test
    @DisplayName("Should return null when mapping null Order to OrderResponse")
    void shouldReturnNullWhenMappingNullOrderToResponse() {
        OrderResponse response = DtoMapper.toOrderResponse(null);
        assertNull(response);
    }
    @Test
    @DisplayName("Should map Event to AvailabilityResponse")
    void shouldMapEventToAvailabilityResponse() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDate = now.plusDays(30);
        Event event = new Event(
            "event-123",
            "Rock Concert",
            eventDate,
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(400),   
            Integer.valueOf(200),   
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(event);
        assertNotNull(response);
        assertEquals("event-123", response.eventId());
        assertEquals("Rock Concert", response.eventName());
        assertEquals(eventDate, response.date());
        assertEquals("Madison Square Garden", response.location());
        assertEquals(1000, response.totalCapacity());
        assertEquals(400, response.availableTickets());
        assertEquals(200, response.reservedTickets());
        assertEquals(400, response.soldTickets());
        assertTrue(response.isAvailable());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
    @Test
    @DisplayName("Should show unavailable when no tickets available")
    void shouldShowUnavailableWhenNoTickets() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDate = now.plusDays(30);
        Event event = new Event(
            "event-123",
            "Sold Out Concert",
            eventDate,
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(0),     
            Integer.valueOf(50),    
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(event);
        assertNotNull(response);
        assertEquals(0, response.availableTickets());
        assertEquals(50, response.reservedTickets());
        assertEquals(950, response.soldTickets());
        assertFalse(response.isAvailable());
        assertEquals(eventDate, response.date());
        assertEquals("Madison Square Garden", response.location());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
    @Test
    @DisplayName("Should calculate sold tickets correctly when all sold")
    void shouldCalculateSoldTicketsWhenAllSold() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDate = now.plusDays(30);
        Event event = new Event(
            "event-123",
            "Completely Sold Out",
            eventDate,
            "Stadium",
            Integer.valueOf(5000),
            Integer.valueOf(0),     
            Integer.valueOf(0),     
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(event);
        assertNotNull(response);
        assertEquals(0, response.availableTickets());
        assertEquals(0, response.reservedTickets());
        assertEquals(5000, response.soldTickets());
        assertFalse(response.isAvailable());
        assertEquals(eventDate, response.date());
        assertEquals("Stadium", response.location());
    }
    @Test
    @DisplayName("Should show available when event is new with all tickets available")
    void shouldShowAvailableWhenEventIsNew() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDate = now.plusDays(60);
        Event event = Event.create("event-123", "New Concert", eventDate, "Arena", 2000);
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(event);
        assertNotNull(response);
        assertEquals(2000, response.availableTickets());
        assertEquals(0, response.reservedTickets());
        assertEquals(0, response.soldTickets());
        assertTrue(response.isAvailable());
        assertEquals(eventDate, response.date());
        assertEquals("Arena", response.location());
    }
    @Test
    @DisplayName("Should return null when mapping null Event to AvailabilityResponse")
    void shouldReturnNullWhenMappingNullEventToAvailabilityResponse() {
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(null);
        assertNull(response);
    }
    @Test
    @DisplayName("Should handle edge case with one available ticket")
    void shouldHandleOneAvailableTicket() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDate = now.plusDays(30);
        Event event = new Event(
            "event-123",
            "Almost Sold Out",
            eventDate,
            "Small Venue",
            Integer.valueOf(100),
            Integer.valueOf(1),     
            Integer.valueOf(10),    
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        AvailabilityResponse response = DtoMapper.toAvailabilityResponse(event);
        assertNotNull(response);
        assertEquals(1, response.availableTickets());
        assertEquals(10, response.reservedTickets());
        assertEquals(89, response.soldTickets());
        assertTrue(response.isAvailable());
        assertEquals(eventDate, response.date());
        assertEquals("Small Venue", response.location());
    }
}
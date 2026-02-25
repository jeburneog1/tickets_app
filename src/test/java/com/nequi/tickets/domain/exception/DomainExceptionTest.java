package com.nequi.tickets.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Exception Tests")
class DomainExceptionTest {
    @Test
    @DisplayName("EventNotFoundException should contain event ID")
    void eventNotFoundExceptionShouldContainEventId() {
        String eventId = "event-123";
        EventNotFoundException exception = new EventNotFoundException(eventId);
        assertNotNull(exception);
        assertEquals(eventId, exception.getEventId());
        assertTrue(exception.getMessage().contains(eventId));
        assertTrue(exception.getMessage().contains("Event not found"));
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }
    @Test
    @DisplayName("OrderNotFoundException should contain order ID")
    void orderNotFoundExceptionShouldContainOrderId() {
        String orderId = "order-456";
        OrderNotFoundException exception = new OrderNotFoundException(orderId);
        assertNotNull(exception);
        assertEquals(orderId, exception.getOrderId());
        assertTrue(exception.getMessage().contains(orderId));
        assertTrue(exception.getMessage().contains("Order not found"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("TicketNotFoundException should contain ticket ID")
    void ticketNotFoundExceptionShouldContainTicketId() {
        String ticketId = "ticket-789";
        TicketNotFoundException exception = new TicketNotFoundException(ticketId);
        assertNotNull(exception);
        assertEquals(ticketId, exception.getTicketId());
        assertTrue(exception.getMessage().contains(ticketId));
        assertTrue(exception.getMessage().contains("Ticket not found"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("InsufficientTicketsException should contain all ticket details")
    void insufficientTicketsExceptionShouldContainAllDetails() {
        String eventId = "event-123";
        int requestedQuantity = 10;
        int availableQuantity = 5;
        InsufficientTicketsException exception = new InsufficientTicketsException(
            eventId, requestedQuantity, availableQuantity
        );
        assertNotNull(exception);
        assertEquals(eventId, exception.getEventId());
        assertEquals(requestedQuantity, exception.getRequestedQuantity());
        assertEquals(availableQuantity, exception.getAvailableQuantity());
        assertTrue(exception.getMessage().contains(eventId));
        assertTrue(exception.getMessage().contains("10"));
        assertTrue(exception.getMessage().contains("5"));
        assertTrue(exception.getMessage().contains("Insufficient tickets"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("MaxTicketsExceededException should contain quantity details")
    void maxTicketsExceededExceptionShouldContainQuantityDetails() {
        int requestedQuantity = 15;
        int maxAllowed = 10;
        MaxTicketsExceededException exception = new MaxTicketsExceededException(
            requestedQuantity, maxAllowed
        );
        assertNotNull(exception);
        assertEquals(requestedQuantity, exception.getRequestedQuantity());
        assertEquals(maxAllowed, exception.getMaxAllowed());
        assertTrue(exception.getMessage().contains("15"));
        assertTrue(exception.getMessage().contains("10"));
        assertTrue(exception.getMessage().contains("exceeds maximum"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("InvalidStateTransitionException should contain all state details")
    void invalidStateTransitionExceptionShouldContainAllDetails() {
        String entityType = "Ticket";
        String entityId = "ticket-123";
        String currentState = "SOLD";
        String attemptedTransition = "RESERVE";
        InvalidStateTransitionException exception = new InvalidStateTransitionException(
            entityType, entityId, currentState, attemptedTransition
        );
        assertNotNull(exception);
        assertEquals(entityType, exception.getEntityType());
        assertEquals(entityId, exception.getEntityId());
        assertEquals(currentState, exception.getCurrentState());
        assertEquals(attemptedTransition, exception.getAttemptedTransition());
        assertTrue(exception.getMessage().contains("Ticket"));
        assertTrue(exception.getMessage().contains("ticket-123"));
        assertTrue(exception.getMessage().contains("SOLD"));
        assertTrue(exception.getMessage().contains("RESERVE"));
        assertTrue(exception.getMessage().contains("Invalid state transition"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("ConcurrentModificationException should contain version details")
    void concurrentModificationExceptionShouldContainVersionDetails() {
        String entityType = "Event";
        String entityId = "event-123";
        Integer expectedVersion = 1;
        Integer actualVersion = 2;
        ConcurrentModificationException exception = new ConcurrentModificationException(
            entityType, entityId, expectedVersion, actualVersion
        );
        assertNotNull(exception);
        assertEquals(entityType, exception.getEntityType());
        assertEquals(entityId, exception.getEntityId());
        assertEquals(expectedVersion, exception.getExpectedVersion());
        assertEquals(actualVersion, exception.getActualVersion());
        assertTrue(exception.getMessage().contains("Event"));
        assertTrue(exception.getMessage().contains("event-123"));
        assertTrue(exception.getMessage().contains("1"));
        assertTrue(exception.getMessage().contains("2"));
        assertTrue(exception.getMessage().contains("Concurrent modification"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("ConcurrentModificationException without version should set versions to null")
    void concurrentModificationExceptionWithoutVersions() {
        String entityType = "Order";
        String entityId = "order-456";
        ConcurrentModificationException exception = new ConcurrentModificationException(
            entityType, entityId
        );
        assertNotNull(exception);
        assertEquals(entityType, exception.getEntityType());
        assertEquals(entityId, exception.getEntityId());
        assertNull(exception.getExpectedVersion());
        assertNull(exception.getActualVersion());
        assertTrue(exception.getMessage().contains("Order"));
        assertTrue(exception.getMessage().contains("order-456"));
        assertTrue(exception.getMessage().contains("Concurrent modification"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("ReservationExpiredException with ticket and order should contain both IDs")
    void reservationExpiredExceptionWithTicketShouldContainBothIds() {
        String ticketId = "ticket-123";
        String orderId = "order-456";
        ReservationExpiredException exception = new ReservationExpiredException(ticketId, orderId);
        assertNotNull(exception);
        assertEquals(ticketId, exception.getTicketId());
        assertEquals(orderId, exception.getOrderId());
        assertTrue(exception.getMessage().contains(ticketId));
        assertTrue(exception.getMessage().contains(orderId));
        assertTrue(exception.getMessage().contains("Reservation expired"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("ReservationExpiredException with only order ID")
    void reservationExpiredExceptionWithOnlyOrderId() {
        String orderId = "order-789";
        ReservationExpiredException exception = new ReservationExpiredException(orderId);
        assertNotNull(exception);
        assertNull(exception.getTicketId());
        assertEquals(orderId, exception.getOrderId());
        assertTrue(exception.getMessage().contains(orderId));
        assertTrue(exception.getMessage().contains("Reservation expired"));
        assertInstanceOf(DomainException.class, exception);
    }
    @Test
    @DisplayName("DomainException should support message with cause")
    void domainExceptionShouldSupportMessageWithCause() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Root cause");
        DomainException exception = new TestDomainException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }
    private static class TestDomainException extends DomainException {
        public TestDomainException(String message) {
            super(message);
        }
        public TestDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
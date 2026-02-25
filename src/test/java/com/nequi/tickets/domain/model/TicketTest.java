package com.nequi.tickets.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ticket Domain Entity Tests")
class TicketTest {
    @Test
    @DisplayName("Should create available ticket using factory method")
    void shouldCreateAvailableTicketUsingFactoryMethod() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        Ticket ticket = Ticket.createAvailableTicket(ticketId, eventId);
        assertNotNull(ticket);
        assertEquals(ticketId, ticket.ticketId());
        assertEquals(eventId, ticket.eventId());
        assertEquals(TicketStatus.AVAILABLE, ticket.status());
        assertNull(ticket.customerId());
        assertNull(ticket.orderId());
        assertNull(ticket.reservedAt());
        assertNull(ticket.reservationExpiresAt());
        assertEquals(0, ticket.version());
        assertNotNull(ticket.createdAt());
        assertNotNull(ticket.updatedAt());
    }
    @Test
    @DisplayName("Should create valid reserved ticket with all required fields")
    void shouldCreateValidReservedTicket() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        String orderId = "order-101";
        LocalDateTime reservedAt = LocalDateTime.now();
        LocalDateTime expiresAt = reservedAt.plusMinutes(Ticket.RESERVATION_TIMEOUT_MINUTES);
        Ticket ticket = new Ticket(
            ticketId, eventId, TicketStatus.RESERVED, customerId, orderId,
            reservedAt, expiresAt, 0, LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(ticket);
        assertEquals(ticketId, ticket.ticketId());
        assertEquals(eventId, ticket.eventId());
        assertEquals(TicketStatus.RESERVED, ticket.status());
        assertEquals(customerId, ticket.customerId());
        assertEquals(orderId, ticket.orderId());
        assertEquals(reservedAt, ticket.reservedAt());
        assertEquals(expiresAt, ticket.reservationExpiresAt());
        assertEquals(0, ticket.version());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when ticket ID is null or blank")
    void shouldThrowExceptionWhenTicketIdIsNullOrBlank(String invalidTicketId) {
        String eventId = "event-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                invalidTicketId, eventId, TicketStatus.AVAILABLE,
                null, null, null, null, 0, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Ticket ID cannot be null or blank", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        String ticketId = "ticket-123";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, invalidEventId, TicketStatus.AVAILABLE,
                null, null, null, null, 0, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Event ID cannot be null or blank", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when ticket status is null")
    void shouldThrowExceptionWhenStatusIsNull() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, eventId, null,
                null, null, null, null, 0, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Ticket status cannot be null", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should throw exception when version is negative")
    void shouldThrowExceptionWhenVersionIsNegative(Integer invalidVersion) {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, eventId, TicketStatus.AVAILABLE,
                null, null, null, null, invalidVersion, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Version cannot be null or negative", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when reserved ticket has no customer ID")
    void shouldThrowExceptionWhenReservedTicketHasNoCustomerId(String invalidCustomerId) {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        LocalDateTime reservedAt = LocalDateTime.now();
        LocalDateTime expiresAt = reservedAt.plusMinutes(10);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, eventId, TicketStatus.RESERVED,
                invalidCustomerId, "order-123", reservedAt, expiresAt, 0,
                LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Customer ID is required for reserved or sold tickets", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when reserved ticket has no expiration time")
    void shouldThrowExceptionWhenReservedTicketHasNoExpirationTime() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, eventId, TicketStatus.RESERVED,
                customerId, "order-123", LocalDateTime.now(), null, 0,
                LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Reservation expiration time is required for reserved tickets", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when pending confirmation ticket has no expiration time")
    void shouldThrowExceptionWhenPendingConfirmationTicketHasNoExpirationTime() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Ticket(
                ticketId, eventId, TicketStatus.PENDING_CONFIRMATION,
                customerId, "order-123", LocalDateTime.now(), null, 0,
                LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Reservation expiration time is required for reserved tickets", exception.getMessage());
    }
    @Test
    @DisplayName("Should create sold ticket with customer ID")
    void shouldCreateSoldTicketWithCustomerId() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        String orderId = "order-101";
        Ticket ticket = new Ticket(
            ticketId, eventId, TicketStatus.SOLD,
            customerId, orderId, null, null, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(ticket);
        assertEquals(TicketStatus.SOLD, ticket.status());
        assertEquals(customerId, ticket.customerId());
        assertEquals(orderId, ticket.orderId());
    }
    @Test
    @DisplayName("Should create complimentary ticket with customer ID")
    void shouldCreateComplimentaryTicketWithCustomerId() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        Ticket ticket = new Ticket(
            ticketId, eventId, TicketStatus.COMPLIMENTARY,
            customerId, null, null, null, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(ticket);
        assertEquals(TicketStatus.COMPLIMENTARY, ticket.status());
        assertEquals(customerId, ticket.customerId());
    }
    @Test
    @DisplayName("Should create ticket with reservation and status reserve using factory method")
    void shouldReserveTicketUsingFactoryMethod() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        String orderId = "order-101";
        Ticket availableTicket = Ticket.createAvailableTicket(ticketId, eventId);
        Ticket reservedTicket = availableTicket.reserveTicket(customerId, orderId);
        assertNotNull(reservedTicket);
        assertEquals(TicketStatus.RESERVED, reservedTicket.status());
        assertEquals(customerId, reservedTicket.customerId());
        assertEquals(orderId, reservedTicket.orderId());
        assertNotNull(reservedTicket.reservedAt());
        assertNotNull(reservedTicket.reservationExpiresAt());
    }
    @Test
    @DisplayName("Should check if reservation is expired")
    void shouldCheckIfReservationIsExpired() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime reservedAt = LocalDateTime.now().minusMinutes(15);
        LocalDateTime expiresAt = reservedAt.plusMinutes(Ticket.RESERVATION_TIMEOUT_MINUTES);
        Ticket expiredTicket = new Ticket(
            ticketId, eventId, TicketStatus.RESERVED,
            customerId, "order-123", reservedAt, expiresAt, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        boolean isExpired = expiredTicket.isReservationExpired();
        assertTrue(isExpired);
    }
    @Test
    @DisplayName("Should check if reservation is not expired")
    void shouldCheckIfReservationIsNotExpired() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime reservedAt = LocalDateTime.now();
        LocalDateTime expiresAt = reservedAt.plusMinutes(Ticket.RESERVATION_TIMEOUT_MINUTES);
        Ticket activeTicket = new Ticket(
            ticketId, eventId, TicketStatus.RESERVED,
            customerId, "order-123", reservedAt, expiresAt, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        boolean isExpired = activeTicket.isReservationExpired();
        assertFalse(isExpired);
    }
    @Test
    @DisplayName("Should transition ticket from pending confirmation to sold")
    void shouldTransitionTicketToSold() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime reservedAt = LocalDateTime.now();
        LocalDateTime expiresAt = reservedAt.plusMinutes(Ticket.RESERVATION_TIMEOUT_MINUTES);
        Ticket pendingTicket = new Ticket(
            ticketId, eventId, TicketStatus.PENDING_CONFIRMATION,
            customerId, "order-123", reservedAt, expiresAt, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        Ticket soldTicket = pendingTicket.confirmTicketSale();
        assertNotNull(soldTicket);
        assertEquals(TicketStatus.SOLD, soldTicket.status());
        assertEquals(customerId, soldTicket.customerId());
        assertNull(soldTicket.reservationExpiresAt()); 
    }
    @Test
    @DisplayName("Should release expired reservation")
    void shouldReleaseExpiredReservation() {
        String ticketId = "ticket-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime reservedAt = LocalDateTime.now().minusMinutes(15);
        LocalDateTime expiresAt = reservedAt.plusMinutes(Ticket.RESERVATION_TIMEOUT_MINUTES);
        Ticket expiredTicket = new Ticket(
            ticketId, eventId, TicketStatus.RESERVED,
            customerId, "order-123", reservedAt, expiresAt, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        Ticket releasedTicket = expiredTicket.releaseTicketBack();
        assertNotNull(releasedTicket);
        assertEquals(TicketStatus.AVAILABLE, releasedTicket.status());
        assertNull(releasedTicket.customerId());
        assertNull(releasedTicket.orderId());
        assertNull(releasedTicket.reservedAt());
        assertNull(releasedTicket.reservationExpiresAt());
    }
    @Test
    @DisplayName("Should have reservation timeout constant set to 10 minutes")
    void shouldHaveReservationTimeoutConstant() {
        assertEquals(10, Ticket.RESERVATION_TIMEOUT_MINUTES);
    }
    @Test
    @DisplayName("Should be immutable - creating new instances for state changes")
    void shouldBeImmutable() {
        Ticket originalTicket = Ticket.createAvailableTicket("ticket-123", "event-456");
        Ticket reservedTicket = originalTicket.reserveTicket("customer-789", "order-101");
        assertNotSame(originalTicket, reservedTicket);
        assertEquals(TicketStatus.AVAILABLE, originalTicket.status());
        assertEquals(TicketStatus.RESERVED, reservedTicket.status());
    }
    @Test
    @DisplayName("Should check if ticket belongs to customer")
    void shouldCheckIfTicketBelongsToCustomer() {
        Ticket ticket = Ticket.createAvailableTicket("ticket-123", "event-456")
                .reserveTicket("customer-789", "order-101");
        assertTrue(ticket.belongsTo("customer-789"));
        assertFalse(ticket.belongsTo("customer-999"));
    }
    @Test
    @DisplayName("Should check if ticket is part of order")
    void shouldCheckIfTicketIsPartOfOrder() {
        Ticket ticket = Ticket.createAvailableTicket("ticket-123", "event-456")
                .reserveTicket("customer-789", "order-101");
        assertTrue(ticket.isPartOfOrder("order-101"));
        assertFalse(ticket.isPartOfOrder("order-999"));
    }
    @Test
    @DisplayName("Should check if ticket is available")
    void shouldCheckIfTicketIsAvailable() {
        Ticket availableTicket = Ticket.createAvailableTicket("ticket-1", "event-1");
        Ticket reservedTicket = availableTicket.reserveTicket("customer-1", "order-1");
        Ticket soldTicket = reservedTicket.startConfirmation().confirmTicketSale();
        assertTrue(availableTicket.isTicketAvailable());
        assertFalse(reservedTicket.isTicketAvailable());
        assertFalse(soldTicket.isTicketAvailable());
    }
}
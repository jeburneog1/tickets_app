package com.nequi.tickets.infrastructure.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateOrderRequest Tests")
class CreateOrderRequestTest {
    @Test
    @DisplayName("Should create valid CreateOrderRequest")
    void shouldCreateValidCreateOrderRequest() {
        String eventId = "event-123";
        String customerId = "customer-456";
        Integer numberOfTickets = 5;
        CreateOrderRequest request = new CreateOrderRequest(eventId, customerId, numberOfTickets);
        assertNotNull(request);
        assertEquals(eventId, request.eventId());
        assertEquals(customerId, request.customerId());
        assertEquals(numberOfTickets, request.numberOfTickets());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or blank event ID")
    void shouldRejectInvalidEventId(String invalidEventId) {
        String customerId = "customer-456";
        Integer numberOfTickets = 5;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateOrderRequest(invalidEventId, customerId, numberOfTickets)
        );
        assertTrue(exception.getMessage().contains("Event ID is required"));
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or blank customer ID")
    void shouldRejectInvalidCustomerId(String invalidCustomerId) {
        String eventId = "event-123";
        Integer numberOfTickets = 5;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateOrderRequest(eventId, invalidCustomerId, numberOfTickets)
        );
        assertTrue(exception.getMessage().contains("Customer ID is required"));
    }
    @Test
    @DisplayName("Should reject null number of tickets")
    void shouldRejectNullNumberOfTickets() {
        String eventId = "event-123";
        String customerId = "customer-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateOrderRequest(eventId, customerId, null)
        );
        assertTrue(exception.getMessage().contains("Number of tickets must be positive"));
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Should reject zero or negative number of tickets")
    void shouldRejectZeroOrNegativeNumberOfTickets(int invalidTickets) {
        String eventId = "event-123";
        String customerId = "customer-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateOrderRequest(eventId, customerId, invalidTickets)
        );
        assertTrue(exception.getMessage().contains("Number of tickets must be positive"));
    }
    @ParameterizedTest
    @ValueSource(ints = {11, 15, 100})
    @DisplayName("Should reject number of tickets exceeding maximum")
    void shouldRejectExcessiveNumberOfTickets(int excessiveTickets) {
        String eventId = "event-123";
        String customerId = "customer-456";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateOrderRequest(eventId, customerId, excessiveTickets)
        );
        assertTrue(exception.getMessage().contains("Maximum 10 tickets per order"));
    }
    @Test
    @DisplayName("Should accept maximum allowed tickets (10)")
    void shouldAcceptMaximumAllowedTickets() {
        String eventId = "event-123";
        String customerId = "customer-456";
        Integer numberOfTickets = 10;
        CreateOrderRequest request = new CreateOrderRequest(eventId, customerId, numberOfTickets);
        assertNotNull(request);
        assertEquals(10, request.numberOfTickets());
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        CreateOrderRequest request1 = new CreateOrderRequest("event-1", "customer-1", 5);
        CreateOrderRequest request2 = new CreateOrderRequest("event-1", "customer-1", 5);
        CreateOrderRequest request3 = new CreateOrderRequest("event-2", "customer-1", 5);
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        CreateOrderRequest request = new CreateOrderRequest("event-123", "customer-456", 5);
        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("event-123"));
        assertTrue(toString.contains("customer-456"));
        assertTrue(toString.contains("5"));
    }
}
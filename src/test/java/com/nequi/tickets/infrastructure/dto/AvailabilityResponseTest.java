package com.nequi.tickets.infrastructure.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AvailabilityResponse Tests")
class AvailabilityResponseTest {
    @Test
    @DisplayName("Should create valid AvailabilityResponse")
    void shouldCreateValidAvailabilityResponse() {
        String eventId = "event-123";
        String eventName = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 700;
        Integer reservedTickets = 200;
        Integer soldTickets = 100;
        Boolean isAvailable = true;
        LocalDateTime now = LocalDateTime.now();
        AvailabilityResponse response = new AvailabilityResponse(
            eventId, eventName, date, location, totalCapacity, availableTickets, 
            reservedTickets, soldTickets, 0, isAvailable, now, now
        );
        assertNotNull(response);
        assertEquals(eventId, response.eventId());
        assertEquals(eventName, response.eventName());
        assertEquals(date, response.date());
        assertEquals(location, response.location());
        assertEquals(totalCapacity, response.totalCapacity());
        assertEquals(availableTickets, response.availableTickets());
        assertEquals(reservedTickets, response.reservedTickets());
        assertEquals(soldTickets, response.soldTickets());
        assertEquals(isAvailable, response.isAvailable());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or blank event ID")
    void shouldRejectInvalidEventId(String invalidEventId) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AvailabilityResponse(
                invalidEventId, "Concert", LocalDateTime.now(), "Location", 
                1000, 700, 200, 100, 0, true, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertTrue(exception.getMessage().contains("Event ID is required"));
    }
    @Test
    @DisplayName("Should allow null other fields")
    void shouldAllowNullOtherFields() {
        String eventId = "event-123";
        AvailabilityResponse response = new AvailabilityResponse(
            eventId, null, null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(response);
        assertEquals(eventId, response.eventId());
        assertNull(response.eventName());
        assertNull(response.date());
        assertNull(response.location());
        assertNull(response.totalCapacity());
        assertNull(response.availableTickets());
        assertNull(response.reservedTickets());
        assertNull(response.soldTickets());
        assertNull(response.isAvailable());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        LocalDateTime now = LocalDateTime.now();
        AvailabilityResponse response1 = new AvailabilityResponse(
            "event-1", "Concert", date, "Location", 1000, 700, 200, 100, 0, true, now, now
        );
        AvailabilityResponse response2 = new AvailabilityResponse(
            "event-1", "Concert", date, "Location", 1000, 700, 200, 100, 0, true, now, now
        );
        AvailabilityResponse response3 = new AvailabilityResponse(
            "event-2", "Concert", date, "Location", 1000, 700, 200, 100, 0, true, now, now
        );
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        LocalDateTime date = LocalDateTime.now();
        AvailabilityResponse response = new AvailabilityResponse(
            "event-123", "Concert", date, "Location", 1000, 700, 200, 100, 0, true, date, date
        );
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("event-123"));
        assertTrue(toString.contains("Concert"));
        assertTrue(toString.contains("1000"));
        assertTrue(toString.contains("700"));
    }
    @Test
    @DisplayName("Should create response with no availability")
    void shouldCreateResponseWithNoAvailability() {
        String eventId = "event-sold-out";
        Boolean isAvailable = false;
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        LocalDateTime now = LocalDateTime.now();
        AvailabilityResponse response = new AvailabilityResponse(
            eventId, "Sold Out Concert", date, "Location", 
            1000, 0, 0, 1000, 0, isAvailable, now, now
        );
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals(0, response.availableTickets());
        assertEquals(1000, response.soldTickets());
    }
}
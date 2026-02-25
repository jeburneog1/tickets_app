package com.nequi.tickets.infrastructure.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventResponse Tests")
class EventResponseTest {
    @Test
    @DisplayName("Should create valid EventResponse")
    void shouldCreateValidEventResponse() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 700;
        Integer reservedTickets = 200;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        EventResponse response = new EventResponse(
            eventId, name, date, location, totalCapacity,
            availableTickets, reservedTickets, Integer.valueOf(0), createdAt, updatedAt
        );
        assertNotNull(response);
        assertEquals(eventId, response.eventId());
        assertEquals(name, response.name());
        assertEquals(date, response.date());
        assertEquals(location, response.location());
        assertEquals(totalCapacity, response.totalCapacity());
        assertEquals(availableTickets, response.availableTickets());
        assertEquals(reservedTickets, response.reservedTickets());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
    @Test
    @DisplayName("Should allow null values")
    void shouldAllowNullValues() {
        EventResponse response = new EventResponse(
            null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(response);
        assertNull(response.eventId());
        assertNull(response.name());
        assertNull(response.date());
        assertNull(response.location());
        assertNull(response.totalCapacity());
        assertNull(response.availableTickets());
        assertNull(response.reservedTickets());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        LocalDateTime timestamp = LocalDateTime.now();
        EventResponse response1 = new EventResponse(
            "event-1", "Concert", date, "NYC", Integer.valueOf(1000), Integer.valueOf(700), Integer.valueOf(200), Integer.valueOf(0), timestamp, timestamp
        );
        EventResponse response2 = new EventResponse(
            "event-1", "Concert", date, "NYC", Integer.valueOf(1000), Integer.valueOf(700), Integer.valueOf(200), Integer.valueOf(0), timestamp, timestamp
        );
        EventResponse response3 = new EventResponse(
            "event-2", "Concert", date, "NYC", Integer.valueOf(1000), Integer.valueOf(700), Integer.valueOf(200), Integer.valueOf(0), timestamp, timestamp
        );
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        
        EventResponse response = new EventResponse(
            "event-123", "Concert", date, "NYC", Integer.valueOf(1000), Integer.valueOf(700), Integer.valueOf(200), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("event-123"));
        assertTrue(toString.contains("Concert"));
        assertTrue(toString.contains("NYC"));
        assertTrue(toString.contains("1000"));
        assertTrue(toString.contains("700"));
    }
    @Test
    @DisplayName("Should handle event with no available tickets")
    void shouldHandleEventWithNoAvailableTickets() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        LocalDateTime timestamp = LocalDateTime.now();
        EventResponse response = new EventResponse(
            "event-sold-out", "Sold Out Concert", date, "Stadium", 
            Integer.valueOf(5000), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), timestamp, timestamp
        );
        assertNotNull(response);
        assertEquals(0, response.availableTickets());
        assertEquals(0, response.reservedTickets());
        assertEquals(5000, response.totalCapacity());
    }
    @Test
    @DisplayName("Should handle event with reserved tickets")
    void shouldHandleEventWithReservedTickets() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        LocalDateTime timestamp = LocalDateTime.now();
        EventResponse response = new EventResponse(
            "event-123", "Popular Concert", date, "Arena", 
            Integer.valueOf(2000), Integer.valueOf(500), Integer.valueOf(1500), Integer.valueOf(0), timestamp, timestamp
        );
        assertNotNull(response);
        assertEquals(500, response.availableTickets());
        assertEquals(1500, response.reservedTickets());
        assertEquals(2000, response.totalCapacity());
    }
}
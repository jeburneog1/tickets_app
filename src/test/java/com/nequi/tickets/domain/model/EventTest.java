package com.nequi.tickets.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Domain Entity Tests")
class EventTest {
    @Test
    @DisplayName("Should create valid event with all required fields")
    void shouldCreateValidEvent() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 800;
        Integer reservedTickets = 200;
        Integer version = 0;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        Event event = new Event(
            eventId, name, date, location, totalCapacity,
            availableTickets, reservedTickets, Integer.valueOf(0), version, createdAt, updatedAt
        );
        assertNotNull(event);
        assertEquals(eventId, event.eventId());
        assertEquals(name, event.name());
        assertEquals(date, event.date());
        assertEquals(location, event.location());
        assertEquals(totalCapacity, event.totalCapacity());
        assertEquals(availableTickets, event.availableTickets());
        assertEquals(reservedTickets, event.reservedTickets());
        assertEquals(version, event.version());
        assertEquals(createdAt, event.createdAt());
        assertEquals(updatedAt, event.updatedAt());
    }
    @Test
    @DisplayName("Should create event using factory method")
    void shouldCreateEventUsingFactoryMethod() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Event event = Event.create(eventId, name, date, location, totalCapacity);
        assertNotNull(event);
        assertEquals(eventId, event.eventId());
        assertEquals(name, event.name());
        assertEquals(date, event.date());
        assertEquals(location, event.location());
        assertEquals(totalCapacity, event.totalCapacity());
        assertEquals(totalCapacity, event.availableTickets());
        assertEquals(0, event.reservedTickets());
        assertEquals(0, event.version());
        assertNotNull(event.createdAt());
        assertNotNull(event.updatedAt());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                invalidEventId, name, date, location, totalCapacity,
                totalCapacity, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Event ID cannot be null or blank", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event name is null or blank")
    void shouldThrowExceptionWhenNameIsNullOrBlank(String invalidName) {
        String eventId = "event-123";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, invalidName, date, location, totalCapacity,
                totalCapacity, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Event name cannot be null or blank", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when date is null")
    void shouldThrowExceptionWhenDateIsNull() {
        String eventId = "event-123";
        String name = "Rock Concert";
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, null, location, totalCapacity,
                totalCapacity, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Event date cannot be null", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when location is null or blank")
    void shouldThrowExceptionWhenLocationIsNullOrBlank(String invalidLocation) {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, invalidLocation, totalCapacity,
                totalCapacity, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Event location cannot be null or blank", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should throw exception when total capacity is zero or negative")
    void shouldThrowExceptionWhenTotalCapacityIsZeroOrNegative(Integer invalidCapacity) {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, invalidCapacity,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Total capacity must be positive", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when total capacity is null")
    void shouldThrowExceptionWhenTotalCapacityIsNull() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, null,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Total capacity must be positive", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should throw exception when available tickets is negative")
    void shouldThrowExceptionWhenAvailableTicketsIsNegative(Integer invalidAvailableTickets) {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, totalCapacity,
                invalidAvailableTickets, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Available tickets cannot be negative", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when available tickets exceeds total capacity")
    void shouldThrowExceptionWhenAvailableTicketsExceedsTotalCapacity() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 1500; 
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, totalCapacity,
                availableTickets, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Available tickets cannot exceed total capacity", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should throw exception when reserved tickets is negative")
    void shouldThrowExceptionWhenReservedTicketsIsNegative(Integer invalidReservedTickets) {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, totalCapacity,
                Integer.valueOf(1000), invalidReservedTickets, Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Reserved tickets cannot be negative", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should throw exception when version is negative")
    void shouldThrowExceptionWhenVersionIsNegative(Integer invalidVersion) {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Event(
                eventId, name, date, location, totalCapacity,
                Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(0), invalidVersion, LocalDateTime.now(), LocalDateTime.now()
            )
        );
        assertEquals("Version cannot be null or negative", exception.getMessage());
    }
    @Test
    @DisplayName("Should create event with zero available tickets")
    void shouldCreateEventWithZeroAvailableTickets() {
        String eventId = "event-123";
        String name = "Sold Out Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 0; 
        Event event = new Event(
            eventId, name, date, location, totalCapacity,
            availableTickets, Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(event);
        assertEquals(0, event.availableTickets());
        assertEquals(1000, event.totalCapacity());
    }
    @Test
    @DisplayName("Should create event with partial availability")
    void shouldCreateEventWithPartialAvailability() {
        String eventId = "event-123";
        String name = "Popular Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Integer availableTickets = 250;
        Integer reservedTickets = 750;
        Event event = new Event(
            eventId, name, date, location, totalCapacity,
            availableTickets, reservedTickets, Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(event);
        assertEquals(250, event.availableTickets());
        assertEquals(750, event.reservedTickets());
        assertEquals(1000, event.totalCapacity());
    }
    @Test
    @DisplayName("Should be immutable - different instances are not equal unless all fields match")
    void shouldBeImmutable() {
        String eventId = "event-123";
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Event event1 = Event.create(eventId, name, date, location, totalCapacity);
        Event event2 = Event.create(eventId, name, date, location, totalCapacity);
        assertNotSame(event1, event2); 
        assertEquals(event1.eventId(), event2.eventId());
        assertEquals(event1.name(), event2.name());
    }
    @Test
    @DisplayName("Should calculate unavailable tickets correctly")
    void shouldCalculateUnavailableTickets() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            1000, 300, 200, 0, 0, LocalDateTime.now(), LocalDateTime.now()
        );
        int unavailable = event.getUnavailableTickets();
        assertEquals(700, unavailable); 
    }
    @Test
    @DisplayName("Should calculate sold tickets correctly")
    void shouldCalculateSoldTickets() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(300), Integer.valueOf(200), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        int sold = event.getSoldTickets();
        assertEquals(500, sold); 
    }
    @Test
    @DisplayName("Should check if has available tickets")
    void shouldCheckHasAvailableTickets() {
        Event eventWithTickets = new Event(
            "event-1", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        Event eventSoldOut = new Event(
            "event-2", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        assertTrue(eventWithTickets.hasAvailableTickets());
        assertFalse(eventSoldOut.hasAvailableTickets());
    }
    @Test
    @DisplayName("Should check if can accommodate quantity")
    void shouldCheckCanAccommodate() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        assertTrue(event.canAccommodate(50));
        assertTrue(event.canAccommodate(100));
        assertFalse(event.canAccommodate(101));
        assertFalse(event.canAccommodate(200));
    }
    @Test
    @DisplayName("Should check if is sold out")
    void shouldCheckIsSoldOut() {
        Event eventWithTickets = new Event(
            "event-1", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        Event eventSoldOut = new Event(
            "event-2", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(900), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        assertFalse(eventWithTickets.isSoldOut());
        assertTrue(eventSoldOut.isSoldOut());
    }
    @Test
    @DisplayName("Should release reserved tickets back to available")
    void shouldReleaseReservedTickets() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            1000, 500, 300, 0, 0, LocalDateTime.now(), LocalDateTime.now()
        );
        Event updated = event.releaseReservedTickets(100);
        assertEquals(600, updated.availableTickets()); 
        assertEquals(200, updated.reservedTickets()); 
        assertEquals(1, updated.version());
    }
    @Test
    @DisplayName("Should throw exception when releasing more than reserved")
    void shouldThrowExceptionWhenReleasingMoreThanReserved() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            1000, 500, 100, 0, 0, LocalDateTime.now(), LocalDateTime.now()
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> event.releaseReservedTickets(200)
        );
        assertTrue(exception.getMessage().contains("Cannot release more than reserved"));
    }
    @Test
    @DisplayName("Should confirm sale by decrementing reserved tickets")
    void shouldConfirmSale() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(500), Integer.valueOf(300), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        Event updated = event.confirmSale(100);
        assertEquals(500, updated.availableTickets()); 
        assertEquals(200, updated.reservedTickets()); 
        assertEquals(1, updated.version());
    }
    @Test
    @DisplayName("Should throw exception when confirming more than reserved")
    void shouldThrowExceptionWhenConfirmingMoreThanReserved() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            Integer.valueOf(1000), Integer.valueOf(500), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), LocalDateTime.now(), LocalDateTime.now()
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> event.confirmSale(200)
        );
        assertTrue(exception.getMessage().contains("Cannot confirm more than reserved"));
    }
    @Test
    @DisplayName("Should throw exception when releasing zero or negative quantity")
    void shouldThrowExceptionWhenReleasingInvalidQuantity() {
        Event event = Event.create("event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC", 1000);
        assertThrows(IllegalArgumentException.class, () -> event.releaseReservedTickets(0));
        assertThrows(IllegalArgumentException.class, () -> event.releaseReservedTickets(-1));
    }
    @Test
    @DisplayName("Should throw exception when confirming zero or negative quantity")
    void shouldThrowExceptionWhenConfirmingInvalidQuantity() {
        Event event = new Event(
            "event-123", "Concert", LocalDateTime.now().plusDays(30), "NYC",
            1000, 500, 300, 0, 0, LocalDateTime.now(), LocalDateTime.now()
        );
        assertThrows(IllegalArgumentException.class, () -> event.confirmSale(0));
        assertThrows(IllegalArgumentException.class, () -> event.confirmSale(-1));
    }
}
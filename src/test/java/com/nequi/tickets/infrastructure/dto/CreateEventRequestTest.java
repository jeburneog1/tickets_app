package com.nequi.tickets.infrastructure.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateEventRequest Tests")
class CreateEventRequestTest {
    @Test
    @DisplayName("Should create valid CreateEventRequest")
    void shouldCreateValidCreateEventRequest() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        CreateEventRequest request = new CreateEventRequest(name, date, location, totalCapacity);
        assertNotNull(request);
        assertEquals(name, request.name());
        assertEquals(date, request.date());
        assertEquals(location, request.location());
        assertEquals(totalCapacity, request.totalCapacity());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or blank name")
    void shouldRejectInvalidName(String invalidName) {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateEventRequest(invalidName, date, location, totalCapacity)
        );
        assertTrue(exception.getMessage().contains("name is required"));
    }
    @Test
    @DisplayName("Should reject null date")
    void shouldRejectNullDate() {
        String name = "Rock Concert";
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateEventRequest(name, null, location, totalCapacity)
        );
        assertTrue(exception.getMessage().contains("date is required"));
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or blank location")
    void shouldRejectInvalidLocation(String invalidLocation) {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        Integer totalCapacity = 1000;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateEventRequest(name, date, invalidLocation, totalCapacity)
        );
        assertTrue(exception.getMessage().contains("location is required"));
    }
    @Test
    @DisplayName("Should reject null capacity")
    void shouldRejectNullCapacity() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateEventRequest(name, date, location, null)
        );
        assertTrue(exception.getMessage().contains("capacity must be positive"));
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should reject zero or negative capacity")
    void shouldRejectZeroOrNegativeCapacity(int invalidCapacity) {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreateEventRequest(name, date, location, invalidCapacity)
        );
        assertTrue(exception.getMessage().contains("capacity must be positive"));
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        
        CreateEventRequest request1 = new CreateEventRequest("Concert", date, "NYC", 1000);
        CreateEventRequest request2 = new CreateEventRequest("Concert", date, "NYC", 1000);
        CreateEventRequest request3 = new CreateEventRequest("Theater", date, "NYC", 1000);
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        CreateEventRequest request = new CreateEventRequest("Concert", date, "NYC", 1000);
        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Concert"));
        assertTrue(toString.contains("NYC"));
        assertTrue(toString.contains("1000"));
    }
}
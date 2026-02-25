package com.nequi.tickets.infrastructure.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {
    @Test
    @DisplayName("Should create ErrorResponse with all fields")
    void shouldCreateErrorResponseWithAllFields() {
        LocalDateTime timestamp = LocalDateTime.now();
        Integer status = 404;
        String error = "Not Found";
        String message = "Event not found";
        String path = "/api/events/123";
        ErrorResponse response = new ErrorResponse(timestamp, status, error, message, path);
        assertNotNull(response);
        assertEquals(timestamp, response.timestamp());
        assertEquals(status, response.status());
        assertEquals(error, response.error());
        assertEquals(message, response.message());
        assertEquals(path, response.path());
    }
    @Test
    @DisplayName("Should create ErrorResponse with convenience constructor")
    void shouldCreateErrorResponseWithConvenienceConstructor() {
        Integer status = 404;
        String error = "Not Found";
        String message = "Event not found";
        String path = "/api/events/123";
        ErrorResponse response = new ErrorResponse(status, error, message, path);
        assertNotNull(response);
        assertNotNull(response.timestamp());
        assertEquals(status, response.status());
        assertEquals(error, response.error());
        assertEquals(message, response.message());
        assertEquals(path, response.path());
        assertTrue(response.timestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
    @Test
    @DisplayName("Should support different HTTP status codes")
    void shouldSupportDifferentHttpStatusCodes() {
        ErrorResponse badRequest = new ErrorResponse(400, "Bad Request", "Invalid input", "/api/orders");
        ErrorResponse notFound = new ErrorResponse(404, "Not Found", "Resource not found", "/api/events/1");
        ErrorResponse conflict = new ErrorResponse(409, "Conflict", "Insufficient tickets", "/api/orders");
        ErrorResponse serverError = new ErrorResponse(500, "Internal Server Error", "Unexpected error", "/api/events");
        assertEquals(400, badRequest.status());
        assertEquals(404, notFound.status());
        assertEquals(409, conflict.status());
        assertEquals(500, serverError.status());
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        ErrorResponse response1 = new ErrorResponse(timestamp, 404, "Not Found", "Event not found", "/api/events/1");
        ErrorResponse response2 = new ErrorResponse(timestamp, 404, "Not Found", "Event not found", "/api/events/1");
        ErrorResponse response3 = new ErrorResponse(timestamp, 400, "Bad Request", "Invalid input", "/api/orders");
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        ErrorResponse response = new ErrorResponse(404, "Not Found", "Event not found", "/api/events/123");
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("404"));
        assertTrue(toString.contains("Not Found"));
        assertTrue(toString.contains("Event not found"));
        assertTrue(toString.contains("/api/events/123"));
    }
    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        ErrorResponse response = new ErrorResponse(null, null, null, null, null);
        assertNotNull(response);
        assertNull(response.timestamp());
        assertNull(response.status());
        assertNull(response.error());
        assertNull(response.message());
        assertNull(response.path());
    }
}
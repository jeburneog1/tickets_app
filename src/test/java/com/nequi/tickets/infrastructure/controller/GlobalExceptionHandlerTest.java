package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.domain.exception.*;
import com.nequi.tickets.infrastructure.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler exceptionHandler;
    private ServerWebExchange exchange;
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        exchange = mock(ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpRequest request = 
            mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath requestPath = 
            mock(org.springframework.http.server.RequestPath.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn("/api/test");
    }
    @Test
    @DisplayName("Should handle EventNotFoundException with 404 status")
    void shouldHandleEventNotFoundException() {
        EventNotFoundException exception = new EventNotFoundException("event-123");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleEventNotFoundException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(404, body.status());
                assertEquals("Not Found", body.error());
                assertTrue(body.message().contains("event-123"));
                assertEquals("/api/test", body.path());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle OrderNotFoundException with 404 status")
    void shouldHandleOrderNotFoundException() {
        OrderNotFoundException exception = new OrderNotFoundException("order-456");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleOrderNotFoundException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(404, body.status());
                assertEquals("Not Found", body.error());
                assertTrue(body.message().contains("order-456"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle TicketNotFoundException with 404 status")
    void shouldHandleTicketNotFoundException() {
        TicketNotFoundException exception = new TicketNotFoundException("ticket-789");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleTicketNotFoundException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(404, body.status());
                assertTrue(body.message().contains("ticket-789"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle InsufficientTicketsException with 409 status")
    void shouldHandleInsufficientTicketsException() {
        InsufficientTicketsException exception = 
            new InsufficientTicketsException("event-123", 10, 5);
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleInsufficientTicketsException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(409, body.status());
                assertEquals("Conflict", body.error());
                assertTrue(body.message().contains("Insufficient"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle MaxTicketsExceededException with 400 status")
    void shouldHandleMaxTicketsExceededException() {
        MaxTicketsExceededException exception = 
            new MaxTicketsExceededException(15, 10);
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleMaxTicketsExceededException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(400, body.status());
                assertEquals("Bad Request", body.error());
                assertTrue(body.message().contains("15"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle ConcurrentModificationException with 409 status")
    void shouldHandleConcurrentModificationException() {
        ConcurrentModificationException exception = 
            new ConcurrentModificationException("Event", "event-123");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleConcurrentModificationException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(409, body.status());
                assertEquals("Conflict", body.error());
                assertTrue(body.message().contains("modified by another process"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle ReservationExpiredException with 410 status")
    void shouldHandleReservationExpiredException() {
        ReservationExpiredException exception = 
            new ReservationExpiredException("ticket-123");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleReservationExpiredException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.GONE, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(410, body.status());
                assertEquals("Gone", body.error());
                assertTrue(body.message().contains("ticket-123"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle InvalidStateTransitionException with 400 status")
    void shouldHandleInvalidStateTransitionException() {
        InvalidStateTransitionException exception = 
            new InvalidStateTransitionException("Ticket", "ticket-123", "SOLD", "AVAILABLE");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleInvalidStateTransitionException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(400, body.status());
                assertTrue(body.message().contains("Ticket"));
                assertTrue(body.message().contains("ticket-123"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException exception = 
            new IllegalArgumentException("Number of tickets must be positive");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleIllegalArgumentException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(400, body.status());
                assertEquals("Bad Request", body.error());
                assertEquals("Number of tickets must be positive", body.message());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle WebExchangeBindException with validation errors")
    void shouldHandleWebExchangeBindException() {
        Object testObject = new Object();
        BeanPropertyBindingResult bindingResult = 
            new BeanPropertyBindingResult(testObject, "testObject");
        bindingResult.addError(new FieldError("testObject", "name", "must not be null"));
        bindingResult.addError(new FieldError("testObject", "capacity", "must be positive"));
        try {
            java.lang.reflect.Method method = this.getClass().getMethod("dummyMethod", Object.class);
            org.springframework.core.MethodParameter methodParameter = 
                new org.springframework.core.MethodParameter(method, 0);
            WebExchangeBindException exception = new WebExchangeBindException(methodParameter, bindingResult);
            Mono<ResponseEntity<ErrorResponse>> result = 
                exceptionHandler.handleValidationErrors(exception, exchange);
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    ErrorResponse body = response.getBody();
                    assertNotNull(body);
                    assertEquals(400, body.status());
                    assertEquals("Validation Error", body.error());
                    assertTrue(body.message().contains("Validation failed"));
                })
                .verifyComplete();
        } catch (NoSuchMethodException e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }
    public void dummyMethod(Object param) {
    }
    @Test
    @DisplayName("Should handle generic DomainException with 400 status")
    void shouldHandleDomainException() {
        DomainException exception = new EventNotFoundException("test-event");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleDomainException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(400, body.status());
                assertEquals("Domain Error", body.error());
                assertTrue(body.message().contains("test-event"));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle generic Exception with 500 status")
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleGenericException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(500, body.status());
                assertEquals("Internal Server Error", body.error());
                assertEquals("An unexpected error occurred", body.message());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should include path in all error responses")
    void shouldIncludePathInErrorResponses() {
        EventNotFoundException exception = new EventNotFoundException("test-123");
        Mono<ResponseEntity<ErrorResponse>> result = 
            exceptionHandler.handleEventNotFoundException(exception, exchange);
        StepVerifier.create(result)
            .assertNext(response -> {
                ErrorResponse body = response.getBody();
                assertNotNull(body);
                assertNotNull(body.path());
                assertEquals("/api/test", body.path());
            })
            .verifyComplete();
    }
}
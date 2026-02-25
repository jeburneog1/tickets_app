package com.nequi.tickets.infrastructure.dto;

import com.nequi.tickets.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderResponse Tests")
class OrderResponseTest {
    @Test
    @DisplayName("Should create valid OrderResponse")
    void shouldCreateValidOrderResponse() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = Arrays.asList("ticket-1", "ticket-2", "ticket-3");
        OrderStatus status = OrderStatus.CONFIRMED;
        Integer totalTickets = 3;
        String failureReason = null;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        LocalDateTime processedAt = LocalDateTime.now();
        OrderResponse response = new OrderResponse(
            orderId, eventId, customerId, ticketIds, status,
            totalTickets, failureReason, createdAt, updatedAt, processedAt
        );
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(eventId, response.eventId());
        assertEquals(customerId, response.customerId());
        assertEquals(ticketIds, response.ticketIds());
        assertEquals(status, response.status());
        assertEquals(totalTickets, response.totalTickets());
        assertNull(response.failureReason());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(processedAt, response.processedAt());
    }
    @Test
    @DisplayName("Should create OrderResponse with failure reason")
    void shouldCreateOrderResponseWithFailureReason() {
        String orderId = "order-failed";
        String failureReason = "Insufficient tickets available";
        OrderStatus status = OrderStatus.FAILED;
        OrderResponse response = new OrderResponse(
            orderId, "event-1", "customer-1", Collections.emptyList(),
            status, 0, failureReason, LocalDateTime.now(), 
            LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(response);
        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals(failureReason, response.failureReason());
        assertTrue(response.ticketIds().isEmpty());
    }
    @Test
    @DisplayName("Should allow null values")
    void shouldAllowNullValues() {
        OrderResponse response = new OrderResponse(
            null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(response);
        assertNull(response.orderId());
        assertNull(response.eventId());
        assertNull(response.customerId());
        assertNull(response.ticketIds());
        assertNull(response.status());
        assertNull(response.totalTickets());
        assertNull(response.failureReason());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
        assertNull(response.processedAt());
    }
    @Test
    @DisplayName("Should support all OrderStatus values")
    void shouldSupportAllOrderStatusValues() {
        LocalDateTime timestamp = LocalDateTime.now();
        OrderResponse pending = new OrderResponse(
            "order-1", "event-1", "customer-1", null,
            OrderStatus.PENDING, 3, null, timestamp, timestamp, null
        );
        OrderResponse confirmed = new OrderResponse(
            "order-2", "event-1", "customer-1", null,
            OrderStatus.CONFIRMED, 3, null, timestamp, timestamp, timestamp
        );
        OrderResponse failed = new OrderResponse(
            "order-3", "event-1", "customer-1", null,
            OrderStatus.FAILED, 3, "Payment failed", timestamp, timestamp, timestamp
        );
        OrderResponse cancelled = new OrderResponse(
            "order-4", "event-1", "customer-1", null,
            OrderStatus.CANCELLED, 3, "Order cancelled", timestamp, timestamp, timestamp
        );
        assertEquals(OrderStatus.PENDING, pending.status());
        assertEquals(OrderStatus.CONFIRMED, confirmed.status());
        assertEquals(OrderStatus.FAILED, failed.status());
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
    }
    @Test
    @DisplayName("Should support record equals and hashCode")
    void shouldSupportRecordEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        List<String> ticketIds = Arrays.asList("ticket-1", "ticket-2");
        OrderResponse response1 = new OrderResponse(
            "order-1", "event-1", "customer-1", ticketIds,
            OrderStatus.CONFIRMED, 2, null, timestamp, timestamp, timestamp
        );
        OrderResponse response2 = new OrderResponse(
            "order-1", "event-1", "customer-1", ticketIds,
            OrderStatus.CONFIRMED, 2, null, timestamp, timestamp, timestamp
        );
        OrderResponse response3 = new OrderResponse(
            "order-2", "event-1", "customer-1", ticketIds,
            OrderStatus.CONFIRMED, 2, null, timestamp, timestamp, timestamp
        );
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
    @Test
    @DisplayName("Should support record toString")
    void shouldSupportRecordToString() {
        List<String> ticketIds = Arrays.asList("ticket-1", "ticket-2");
        OrderResponse response = new OrderResponse(
            "order-123", "event-456", "customer-789", ticketIds,
            OrderStatus.CONFIRMED, 2, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("order-123"));
        assertTrue(toString.contains("event-456"));
        assertTrue(toString.contains("customer-789"));
        assertTrue(toString.contains("CONFIRMED"));
    }
    @Test
    @DisplayName("Should handle empty ticket list")
    void shouldHandleEmptyTicketList() {
        OrderResponse response = new OrderResponse(
            "order-pending", "event-1", "customer-1",
            Collections.emptyList(), OrderStatus.PENDING, 0, null,
            LocalDateTime.now(), LocalDateTime.now(), null
        );
        assertNotNull(response);
        assertNotNull(response.ticketIds());
        assertTrue(response.ticketIds().isEmpty());
        assertEquals(0, response.totalTickets());
    }
    @Test
    @DisplayName("Should handle large ticket list")
    void shouldHandleLargeTicketList() {
        List<String> ticketIds = Arrays.asList(
            "ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5",
            "ticket-6", "ticket-7", "ticket-8", "ticket-9", "ticket-10"
        );
        OrderResponse response = new OrderResponse(
            "order-large", "event-1", "customer-1", ticketIds,
            OrderStatus.CONFIRMED, 10, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        assertNotNull(response);
        assertEquals(10, response.ticketIds().size());
        assertEquals(10, response.totalTickets());
    }
}
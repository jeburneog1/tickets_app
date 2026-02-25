package com.nequi.tickets.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Domain Entity Tests")
class OrderTest {
    @Test
    @DisplayName("Should create pending order using factory method")
    void shouldCreatePendingOrderUsingFactoryMethod() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1", "ticket-2", "ticket-3");
        Order order = Order.createPending(orderId, eventId, customerId, ticketIds);
        assertNotNull(order);
        assertEquals(orderId, order.orderId());
        assertEquals(eventId, order.eventId());
        assertEquals(customerId, order.customerId());
        assertEquals(ticketIds, order.ticketIds());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(3, order.totalTickets());
        assertEquals(0, order.retryCount());
        assertEquals(0, order.version());
        assertNull(order.failureReason());
        assertNotNull(order.createdAt());
        assertNotNull(order.updatedAt());
        assertNull(order.processedAt());
    }
    @Test
    @DisplayName("Should create valid order with all required fields")
    void shouldCreateValidOrder() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1", "ticket-2");
        OrderStatus status = OrderStatus.CONFIRMED;
        Integer totalTickets = 2;
        Integer retryCount = 0;
        Integer version = 1;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        LocalDateTime processedAt = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, customerId, ticketIds, status,
            totalTickets, retryCount, version, null,
            createdAt, updatedAt, processedAt
        );
        assertNotNull(order);
        assertEquals(orderId, order.orderId());
        assertEquals(eventId, order.eventId());
        assertEquals(customerId, order.customerId());
        assertEquals(ticketIds, order.ticketIds());
        assertEquals(status, order.status());
        assertEquals(totalTickets, order.totalTickets());
        assertEquals(retryCount, order.retryCount());
        assertEquals(version, order.version());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when order ID is null or blank")
    void shouldThrowExceptionWhenOrderIdIsNullOrBlank(String invalidOrderId) {
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                invalidOrderId, eventId, customerId, ticketIds, OrderStatus.PENDING,
                1, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Order ID cannot be null or blank", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        String orderId = "order-123";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, invalidEventId, customerId, ticketIds, OrderStatus.PENDING,
                1, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Event ID cannot be null or blank", exception.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when customer ID is null or blank")
    void shouldThrowExceptionWhenCustomerIdIsNullOrBlank(String invalidCustomerId) {
        String orderId = "order-123";
        String eventId = "event-456";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, invalidCustomerId, ticketIds, OrderStatus.PENDING,
                1, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Customer ID cannot be null or blank", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when ticket IDs list is null")
    void shouldThrowExceptionWhenTicketIdsIsNull() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, null, OrderStatus.PENDING,
                0, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Order must have at least one ticket", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when ticket IDs list is empty")
    void shouldThrowExceptionWhenTicketIdsIsEmpty() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> emptyTicketIds = Collections.emptyList();
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, emptyTicketIds, OrderStatus.PENDING,
                0, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Order must have at least one ticket", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when order exceeds maximum tickets")
    void shouldThrowExceptionWhenOrderExceedsMaximumTickets() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> tooManyTickets = new ArrayList<>();
        for (int i = 0; i < Order.MAX_TICKETS_PER_ORDER + 1; i++) {
            tooManyTickets.add("ticket-" + i);
        }
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createPending(orderId, eventId, customerId, tooManyTickets)
        );
        assertTrue(exception.getMessage().contains("Cannot create order with more than"));
    }
    @Test
    @DisplayName("Should create order with maximum allowed tickets")
    void shouldCreateOrderWithMaximumAllowedTickets() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> maxTickets = new ArrayList<>();
        for (int i = 0; i < Order.MAX_TICKETS_PER_ORDER; i++) {
            maxTickets.add("ticket-" + i);
        }
        Order order = Order.createPending(orderId, eventId, customerId, maxTickets);
        assertNotNull(order);
        assertEquals(Order.MAX_TICKETS_PER_ORDER, order.totalTickets());
    }
    @Test
    @DisplayName("Should throw exception when order status is null")
    void shouldThrowExceptionWhenStatusIsNull() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, ticketIds, null,
                1, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Order status cannot be null", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should throw exception when total tickets is zero or negative")
    void shouldThrowExceptionWhenTotalTicketsIsZeroOrNegative(Integer invalidTotal) {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, ticketIds, OrderStatus.PENDING,
                invalidTotal, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Total tickets must be positive", exception.getMessage());
    }
    @Test
    @DisplayName("Should throw exception when total tickets doesn't match ticket IDs count")
    void shouldThrowExceptionWhenTotalTicketsDoesNotMatchTicketIdsCount() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1", "ticket-2");
        Integer incorrectTotal = 5;
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, ticketIds, OrderStatus.PENDING,
                incorrectTotal, 0, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Total tickets must match ticket IDs count", exception.getMessage());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should throw exception when retry count is negative")
    void shouldThrowExceptionWhenRetryCountIsNegative(Integer invalidRetryCount) {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                orderId, eventId, customerId, ticketIds, OrderStatus.PENDING,
                1, invalidRetryCount, 0, null, LocalDateTime.now(), LocalDateTime.now(), null
            )
        );
        assertEquals("Retry count cannot be null or negative", exception.getMessage());
    }
    @Test
    @DisplayName("Should transition order from pending to processing")
    void shouldTransitionOrderFromPendingToProcessing() {
        Order pendingOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        Order processingOrder = pendingOrder.startProcessing();
        assertNotNull(processingOrder);
        assertEquals(OrderStatus.PROCESSING, processingOrder.status());
        assertEquals(1, processingOrder.retryCount());
        assertEquals(pendingOrder.version() + 1, processingOrder.version());
    }
    @Test
    @DisplayName("Should transition order from processing to confirmed")
    void shouldTransitionOrderFromProcessingToConfirmed() {
        Order pendingOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        Order processingOrder = pendingOrder.startProcessing();
        Order confirmedOrder = processingOrder.confirm();
        assertNotNull(confirmedOrder);
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.status());
        assertNotNull(confirmedOrder.processedAt());
        assertNull(confirmedOrder.failureReason());
    }
    @Test
    @DisplayName("Should transition order from processing to failed with reason")
    void shouldTransitionOrderFromProcessingToFailed() {
        Order pendingOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        Order processingOrder = pendingOrder.startProcessing();
        String failureReason = "Insufficient tickets available";
        Order failedOrder = processingOrder.fail(failureReason);
        assertNotNull(failedOrder);
        assertEquals(OrderStatus.FAILED, failedOrder.status());
        assertEquals(failureReason, failedOrder.failureReason());
        assertNotNull(failedOrder.processedAt());
    }
    @Test
    @DisplayName("Should cancel pending order")
    void shouldCancelPendingOrder() {
        Order pendingOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        String cancellationReason = "Customer requested cancellation";
        Order cancelledOrder = pendingOrder.cancel(cancellationReason);
        assertNotNull(cancelledOrder);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.status());
        assertEquals(cancellationReason, cancelledOrder.failureReason());
    }
    @Test
    @DisplayName("Should increment retry count")
    void shouldIncrementRetryCount() {
        Order order = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        Order retriedOrder = order.incrementRetry();
        assertNotNull(retriedOrder);
        assertEquals(1, retriedOrder.retryCount());
        assertEquals(order.version() + 1, retriedOrder.version());
    }
    @Test
    @DisplayName("Should throw exception when exceeding maximum retries")
    void shouldThrowExceptionWhenExceedingMaximumRetries() {
        Order order = new Order(
            "order-123", "event-456", "customer-789", List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            3,
            0,
            null,
            LocalDateTime.now(), LocalDateTime.now(), null
        );
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.incrementRetry()
        );
        assertTrue(exception.getMessage().contains("already been retried maximum times"));
    }
    @Test
    @DisplayName("Should check if order has exceeded max retries")
    void shouldCheckIfOrderHasExceededMaxRetries() {
        Order orderWithMaxRetries = new Order(
            "order-123", "event-456", "customer-789", List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            3,
            0,
            null,
            LocalDateTime.now(), LocalDateTime.now(), null
        );
        Order orderUnderLimit = new Order(
            "order-456", "event-456", "customer-789", List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            Order.MAX_RETRY_ATTEMPTS - 1,
            0,
            null,
            LocalDateTime.now(), LocalDateTime.now(), null
        );
        assertTrue(orderWithMaxRetries.hasExceededMaxRetries());
        assertFalse(orderUnderLimit.hasExceededMaxRetries());
    }
    @Test
    @DisplayName("Should check if order can be retried")
    void shouldCheckIfOrderCanBeRetried() {
        Order processingOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        ).startProcessing();
        Order confirmedOrder = processingOrder.confirm();
        assertTrue(processingOrder.canRetry());
        assertFalse(confirmedOrder.canRetry());
    }
    @Test
    @DisplayName("Should check if order belongs to customer")
    void shouldCheckIfOrderBelongsToCustomer() {
        String customerId = "customer-789";
        Order order = Order.createPending(
            "order-123", "event-456", customerId, List.of("ticket-1")
        );
        assertTrue(order.belongsTo(customerId));
        assertFalse(order.belongsTo("other-customer"));
    }
    @Test
    @DisplayName("Should check if order is for event")
    void shouldCheckIfOrderIsForEvent() {
        String eventId = "event-456";
        Order order = Order.createPending(
            "order-123", eventId, "customer-789", List.of("ticket-1")
        );
        assertTrue(order.isForEvent(eventId));
        assertFalse(order.isForEvent("other-event"));
    }
    @Test
    @DisplayName("Should return immutable copy of ticket IDs")
    void shouldReturnImmutableCopyOfTicketIds() {
        List<String> ticketIds = new ArrayList<>(List.of("ticket-1", "ticket-2"));
        Order order = Order.createPending(
            "order-123", "event-456", "customer-789", ticketIds
        );
        List<String> returnedTicketIds = order.ticketIds();
        ticketIds.add("ticket-3"); 
        assertEquals(2, returnedTicketIds.size());
        assertThrows(UnsupportedOperationException.class, () -> returnedTicketIds.add("ticket-4"));
    }
    @Test
    @DisplayName("Should have max tickets per order constant set to 10")
    void shouldHaveMaxTicketsPerOrderConstant() {
        assertEquals(10, Order.MAX_TICKETS_PER_ORDER);
    }
    @Test
    @DisplayName("Should have max retry attempts constant set to 3")
    void shouldHaveMaxRetryAttemptsConstant() {
        assertEquals(3, Order.MAX_RETRY_ATTEMPTS);
    }
    @Test
    @DisplayName("Should be immutable - creating new instances for state changes")
    void shouldBeImmutable() {
        Order originalOrder = Order.createPending(
            "order-123", "event-456", "customer-789", List.of("ticket-1")
        );
        Order processingOrder = originalOrder.startProcessing();
        assertNotSame(originalOrder, processingOrder);
        assertEquals(OrderStatus.PENDING, originalOrder.status());
        assertEquals(OrderStatus.PROCESSING, processingOrder.status());
    }
}
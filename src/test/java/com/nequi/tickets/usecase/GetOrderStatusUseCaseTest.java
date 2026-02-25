package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderStatusUseCase Tests")
class GetOrderStatusUseCaseTest {
    @Mock
    private OrderRepository orderRepository;
    private GetOrderStatusUseCase getOrderStatusUseCase;
    @BeforeEach
    void setUp() {
        getOrderStatusUseCase = new GetOrderStatusUseCase(orderRepository);
    }
    @Test
    @DisplayName("Should get order status successfully when order exists")
    void shouldGetOrderStatusSuccessfully() {
        String orderId = "order-123";
        Order order = Order.createPending(orderId, "event-456", "customer-789", List.of("ticket-1", "ticket-2"));
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(retrievedOrder -> {
                assertNotNull(retrievedOrder);
                assertEquals(orderId, retrievedOrder.orderId());
                assertEquals(OrderStatus.PENDING, retrievedOrder.status());
                assertEquals("event-456", retrievedOrder.eventId());
                assertEquals("customer-789", retrievedOrder.customerId());
            })
            .verifyComplete();
        verify(orderRepository, times(1)).findById(orderId);
    }
    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        String orderId = "non-existent-order";
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof OrderNotFoundException &&
                throwable.getMessage().contains("Order not found with ID: " + orderId))
            .verify();
        verify(orderRepository, times(1)).findById(orderId);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw IllegalArgumentException when order ID is null or blank")
    void shouldThrowExceptionWhenOrderIdIsNullOrBlank(String invalidOrderId) {
        Mono<Order> result = getOrderStatusUseCase.execute(invalidOrderId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Order ID is required"))
            .verify();
        verifyNoInteractions(orderRepository);
    }
    @Test
    @DisplayName("Should return order with PROCESSING status")
    void shouldReturnOrderWithProcessingStatus() {
        String orderId = "order-123";
        Order pendingOrder = Order.createPending(orderId, "event-456", "customer-789", List.of("ticket-1"));
        Order processingOrder = pendingOrder.startProcessing();
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(order -> assertEquals(OrderStatus.PROCESSING, order.status()))
            .verifyComplete();
    }
    @Test
    @DisplayName("Should return order with CONFIRMED status")
    void shouldReturnOrderWithConfirmedStatus() {
        String orderId = "order-123";
        Order pendingOrder = Order.createPending(orderId, "event-456", "customer-789", List.of("ticket-1"));
        Order processingOrder = pendingOrder.startProcessing();
        Order confirmedOrder = processingOrder.confirm();
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(confirmedOrder));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(OrderStatus.CONFIRMED, order.status());
                assertNotNull(order.processedAt());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should return order with FAILED status")
    void shouldReturnOrderWithFailedStatus() {
        String orderId = "order-123";
        Order pendingOrder = Order.createPending(orderId, "event-456", "customer-789", List.of("ticket-1"));
        Order processingOrder = pendingOrder.startProcessing();
        Order failedOrder = processingOrder.fail("Insufficient tickets");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(failedOrder));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(OrderStatus.FAILED, order.status());
                assertEquals("Insufficient tickets", order.failureReason());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should return order with CANCELLED status")
    void shouldReturnOrderWithCancelledStatus() {
        String orderId = "order-123";
        Order pendingOrder = Order.createPending(orderId, "event-456", "customer-789", List.of("ticket-1"));
        Order cancelledOrder = pendingOrder.cancel("Customer requested cancellation");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(cancelledOrder));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(OrderStatus.CANCELLED, order.status());
                assertEquals("Customer requested cancellation", order.failureReason());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should return complete order with all fields")
    void shouldReturnCompleteOrderWithAllFields() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        List<String> ticketIds = List.of("ticket-1", "ticket-2", "ticket-3");
        Order order = Order.createPending(orderId, eventId, customerId, ticketIds);
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .assertNext(retrievedOrder -> {
                assertEquals(orderId, retrievedOrder.orderId());
                assertEquals(eventId, retrievedOrder.eventId());
                assertEquals(customerId, retrievedOrder.customerId());
                assertEquals(ticketIds, retrievedOrder.ticketIds());
                assertEquals(3, retrievedOrder.totalTickets());
                assertEquals(0, retrievedOrder.retryCount());
                assertEquals(0, retrievedOrder.version());
                assertNotNull(retrievedOrder.createdAt());
                assertNotNull(retrievedOrder.updatedAt());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle repository error")
    void shouldHandleRepositoryError() {
        String orderId = "order-123";
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(orderRepository.findById(orderId)).thenReturn(Mono.error(repositoryError));
        Mono<Order> result = getOrderStatusUseCase.execute(orderId);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(orderRepository, times(1)).findById(orderId);
    }
    @Test
    @DisplayName("Should handle multiple sequential calls independently")
    void shouldHandleMultipleSequentialCallsIndependently() {
        String orderId1 = "order-123";
        String orderId2 = "order-456";
        Order order1 = Order.createPending(orderId1, "event-1", "customer-1", List.of("ticket-1"));
        Order order2 = Order.createPending(orderId2, "event-2", "customer-2", List.of("ticket-2"));
        when(orderRepository.findById(orderId1)).thenReturn(Mono.just(order1));
        when(orderRepository.findById(orderId2)).thenReturn(Mono.just(order2));
        StepVerifier.create(getOrderStatusUseCase.execute(orderId1))
            .assertNext(order -> assertEquals(orderId1, order.orderId()))
            .verifyComplete();
        StepVerifier.create(getOrderStatusUseCase.execute(orderId2))
            .assertNext(order -> assertEquals(orderId2, order.orderId()))
            .verifyComplete();
        verify(orderRepository, times(1)).findById(orderId1);
        verify(orderRepository, times(1)).findById(orderId2);
    }
}
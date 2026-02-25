package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.infrastructure.dto.CreateOrderRequest;
import com.nequi.tickets.usecase.ConfirmOrderUseCase;
import com.nequi.tickets.usecase.CreateOrderUseCase;
import com.nequi.tickets.usecase.GetOrderStatusUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {
    @Mock
    private CreateOrderUseCase createOrderUseCase;
    @Mock
    private GetOrderStatusUseCase getOrderStatusUseCase;
    @Mock
    private ConfirmOrderUseCase confirmOrderUseCase;
    private WebTestClient webTestClient;
    @BeforeEach
    void setUp() {
        OrderController orderController = new OrderController(
            createOrderUseCase,
            getOrderStatusUseCase,
            confirmOrderUseCase
        );
        webTestClient = WebTestClient.bindToController(orderController)
            .controllerAdvice(new GlobalExceptionHandler())
            .build();
    }
    @Test
    @DisplayName("POST /orders - Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        CreateOrderRequest request = new CreateOrderRequest(
            "event-123",
            "customer-456",
            5
        );
        LocalDateTime now = LocalDateTime.now();
        Order createdOrder = new Order(
            "order-789",
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.PENDING,
            5,
            0,  
            0,  
            null,
            now,
            now,
            null
        );
        when(createOrderUseCase.execute(anyString(), anyString(), anyInt()))
            .thenReturn(Mono.just(createdOrder));
        webTestClient.post()
            .uri("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo("order-789")
            .jsonPath("$.eventId").isEqualTo("event-123")
            .jsonPath("$.customerId").isEqualTo("customer-456")
            .jsonPath("$.status").isEqualTo("PENDING")
            .jsonPath("$.totalTickets").isEqualTo(5)
            .jsonPath("$.ticketIds").isArray();
        verify(createOrderUseCase).execute("event-123", "customer-456", 5);
    }
    @Test
    @DisplayName("POST /orders - Should handle order with single ticket")
    void shouldHandleOrderWithSingleTicket() {
        CreateOrderRequest request = new CreateOrderRequest(
            "event-123",
            "customer-456",
            1
        );
        LocalDateTime now = LocalDateTime.now();
        Order createdOrder = new Order(
            "order-789",
            "event-123",
            "customer-456",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            0,  
            0,  
            null,
            now,
            now,
            null
        );
        when(createOrderUseCase.execute(anyString(), anyString(), anyInt()))
            .thenReturn(Mono.just(createdOrder));
        webTestClient.post()
            .uri("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo("order-789")
            .jsonPath("$.totalTickets").isEqualTo(1)
            .jsonPath("$.status").isEqualTo("PENDING");
        verify(createOrderUseCase).execute("event-123", "customer-456", 1);
    }
    @Test
    @DisplayName("GET /orders/{orderId} - Should return pending order status")
    void shouldReturnPendingOrderStatus() {
        String orderId = "order-789";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId,
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.PENDING,
            5,
            0,  
            0,  
            null,
            now,
            now,
            null
        );
        when(getOrderStatusUseCase.execute(orderId)).thenReturn(Mono.just(order));
        webTestClient.get()
            .uri("/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo(orderId)
            .jsonPath("$.status").isEqualTo("PENDING")
            .jsonPath("$.totalTickets").isEqualTo(5)
            .jsonPath("$.failureReason").doesNotExist()
            .jsonPath("$.processedAt").doesNotExist();
        verify(getOrderStatusUseCase).execute(orderId);
    }
    @Test
    @DisplayName("GET /orders/{orderId} - Should return confirmed order status with tickets")
    void shouldReturnConfirmedOrderStatus() {
        String orderId = "order-789";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processedAt = now.plusMinutes(1);
        Order order = new Order(
            orderId,
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3"),
            OrderStatus.CONFIRMED,
            3,
            0,  
            0,  
            null,
            now,
            processedAt,
            processedAt
        );
        when(getOrderStatusUseCase.execute(orderId)).thenReturn(Mono.just(order));
        webTestClient.get()
            .uri("/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo(orderId)
            .jsonPath("$.status").isEqualTo("CONFIRMED")
            .jsonPath("$.totalTickets").isEqualTo(3)
            .jsonPath("$.ticketIds").isArray()
            .jsonPath("$.ticketIds.length()").isEqualTo(3)
            .jsonPath("$.ticketIds[0]").isEqualTo("ticket-1")
            .jsonPath("$.ticketIds[1]").isEqualTo("ticket-2")
            .jsonPath("$.ticketIds[2]").isEqualTo("ticket-3")
            .jsonPath("$.processedAt").exists();
        verify(getOrderStatusUseCase).execute(orderId);
    }
    @Test
    @DisplayName("GET /orders/{orderId} - Should return failed order status with reason")
    void shouldReturnFailedOrderStatus() {
        String orderId = "order-789";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processedAt = now.plusMinutes(1);
        Order order = new Order(
            orderId,
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.FAILED,
            5,
            0,  
            0,  
            "Insufficient tickets available",
            now,
            processedAt,
            processedAt
        );
        when(getOrderStatusUseCase.execute(orderId)).thenReturn(Mono.just(order));
        webTestClient.get()
            .uri("/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo(orderId)
            .jsonPath("$.status").isEqualTo("FAILED")
            .jsonPath("$.totalTickets").isEqualTo(5)
            .jsonPath("$.failureReason").isEqualTo("Insufficient tickets available")
            .jsonPath("$.processedAt").exists();
        verify(getOrderStatusUseCase).execute(orderId);
    }
    @Test
    @DisplayName("GET /orders/{orderId} - Should return cancelled order status")
    void shouldReturnCancelledOrderStatus() {
        String orderId = "order-789";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processedAt = now.plusMinutes(15);
        Order order = new Order(
            orderId,
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.CANCELLED,
            5,
            0,  
            0,  
            "Reservation expired",
            now,
            processedAt,
            null
        );
        when(getOrderStatusUseCase.execute(orderId)).thenReturn(Mono.just(order));
        webTestClient.get()
            .uri("/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo(orderId)
            .jsonPath("$.status").isEqualTo("CANCELLED")
            .jsonPath("$.failureReason").isEqualTo("Reservation expired");
        verify(getOrderStatusUseCase).execute(orderId);
    }
    @Test
    @DisplayName("GET /orders/{orderId} - Should return processing order status")
    void shouldReturnProcessingOrderStatus() {
        String orderId = "order-789";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId,
            "event-123",
            "customer-456",
            List.of("ticket-1", "ticket-2", "ticket-3", "ticket-4", "ticket-5"),
            OrderStatus.PROCESSING,
            5,
            0,  
            0,  
            null,
            now,
            now,
            null
        );
        when(getOrderStatusUseCase.execute(orderId)).thenReturn(Mono.just(order));
        webTestClient.get()
            .uri("/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.orderId").isEqualTo(orderId)
            .jsonPath("$.status").isEqualTo("PROCESSING")
            .jsonPath("$.totalTickets").isEqualTo(5);
        verify(getOrderStatusUseCase).execute(orderId);
    }
}
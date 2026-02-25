package com.nequi.tickets.usecase;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.exception.InvalidStateTransitionException;
import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderUseCaseTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private BusinessProperties businessProperties;
    
    @InjectMocks
    private ProcessOrderUseCase useCase;
    
    @BeforeEach
    void setUp() {
        BusinessProperties.Order order = new BusinessProperties.Order();
        order.setMaxRetries(3);
        lenient().when(businessProperties.getOrder()).thenReturn(order);
    }
    @Test
    void execute_withValidOrder_shouldProcessSuccessfully() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime now = LocalDateTime.now();
        Order pendingOrder = new Order(
            orderId,
            eventId,
            customerId,
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PENDING,
            2,
            0,
            0,
            null,
            now,
            now,
            null
        );
        Order processingOrder = pendingOrder.startProcessing();
        Ticket ticket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            customerId, orderId, now, now.plusMinutes(10),
            1, now, now
        );
        Ticket ticket2 = new Ticket(
            "ticket-2", eventId, TicketStatus.RESERVED,
            customerId, orderId, now, now.plusMinutes(10),
            1, now, now
        );
        Ticket ticket1Pending = ticket1.startConfirmation();
        Ticket ticket2Pending = ticket2.startConfirmation();
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.just(ticket1, ticket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket1Pending, ticket2Pending));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.orderId()).isEqualTo(orderId);
                assertThat(result.status()).isEqualTo(OrderStatus.PROCESSING);
            })
            .verifyComplete();
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).findByOrderId(anyString());
        verify(ticketRepository).saveAll(anyList());
    }
    @Test
    void execute_withNullOrderId_shouldReturnError() {
        StepVerifier.create(useCase.execute(null))
            .expectErrorMatches(error -> 
                error instanceof IllegalArgumentException &&
                error.getMessage().equals("Order ID is required"))
            .verify();
        verify(orderRepository, never()).findById(anyString());
    }
    @Test
    void execute_withBlankOrderId_shouldReturnError() {
        StepVerifier.create(useCase.execute("  "))
            .expectErrorMatches(error -> 
                error instanceof IllegalArgumentException &&
                error.getMessage().equals("Order ID is required"))
            .verify();
    }
    @Test
    void execute_withNonExistentOrder_shouldReturnOrderNotFoundError() {
        String orderId = "non-existent";
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());
        StepVerifier.create(useCase.execute(orderId))
            .expectError(OrderNotFoundException.class)
            .verify();
        verify(orderRepository).findById(orderId);
    }
    @Test
    void execute_withAlreadyConfirmedOrder_shouldReturnOrderImmediately() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();
        Order confirmedOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.CONFIRMED,
            1,
            1,
            1,
            "Cancelled by customer",
            now,
            now,
            now
        );
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(confirmedOrder));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
            })
            .verifyComplete();
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(ticketRepository, never()).findByIds(anyList());
    }
    @Test
    void execute_withFailedOrder_shouldReturnOrderImmediately() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();
        Order failedOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.FAILED,
            1,
            1,
            1,
            "Max retries exceeded",
            now,
            now,
            null
        );
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(failedOrder));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
            })
            .verifyComplete();
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void execute_withCancelledOrder_shouldReturnOrderImmediately() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();
        Order cancelledOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.CANCELLED,
            1,
            1,
            1,
            "Order cancelled",
            now,
            now,
            null
        );
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(cancelledOrder));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
            })
            .verifyComplete();
    }
    @Test
    void execute_withMaxRetriesExceeded_shouldFailOrder() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            3,
            1,
            null,
            now,
            now,
            null
        );
        Order failedOrder = order.fail("Maximum retry attempts exceeded");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(failedOrder));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
                assertThat(result.failureReason()).contains("Maximum retry attempts exceeded");
            })
            .verifyComplete();
        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void execute_withInsufficientTicketsFound_shouldFailWithError() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PENDING,
            2,
            0,
            0,
            null,
            now,
            now,
            null
        );
        Order processingOrder = order.startProcessing();
        Ticket ticket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now, now.plusMinutes(10),
            1, now, now
        );
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.just(ticket1)); 
        StepVerifier.create(useCase.execute(orderId))
            .expectError(InsufficientTicketsException.class)
            .verify();
    }
    @Test
    void execute_withAlreadySoldTickets_shouldProcessIdempotently() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            1,
            0,
            null,
            now,
            now,
            null
        );
        Order processingOrder = order.startProcessing();
        Ticket soldTicket = new Ticket(
            "ticket-1", eventId, TicketStatus.SOLD, 
            "customer-123", orderId, now, now.plusMinutes(10),
            1, now, now
        );
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.just(soldTicket));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.PROCESSING);
            })
            .verifyComplete();
    }
    @Test
    void execute_withProcessingError_shouldPropagateErrorForRetry() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            1,
            0,
            null,
            now,
            now,
            null
        );
        Order processingOrder = order.startProcessing();
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.error(new RuntimeException("Database error")));
        StepVerifier.create(useCase.execute(orderId))
            .expectErrorMatches(error -> 
                error instanceof RuntimeException &&
                error.getMessage().equals("Database error"))
            .verify();
    }
    @Test
    void execute_withProcessingErrorAtMaxRetries_shouldFailOrder() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            2, 
            1,
            null,
            now,
            now,
            null
        );
        Order processingOrder = order.startProcessing(); 
        Order failedOrder = processingOrder.fail("Processing failed after max retries: Database error");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(Mono.just(processingOrder))
            .thenReturn(Mono.just(failedOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.error(new RuntimeException("Database error")));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
                assertThat(result.failureReason()).contains("Database error");
            })
            .verifyComplete();
    }
    @Test
    void execute_withEventInventoryUpdateRetry_shouldSucceedAfterRetry() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            1,
            0,
            null,
            now,
            now,
            null
        );
        Order processingOrder = order.startProcessing();
        Ticket ticket = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now, now.plusMinutes(10),
            1, now, now
        );
        Ticket ticketPending = ticket.startConfirmation();
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(anyString())).thenReturn(Flux.just(ticket));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticketPending));
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.PROCESSING);
            })
            .verifyComplete();
        verify(ticketRepository).saveAll(anyList());
    }
}
package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.InvalidStateTransitionException;
import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ConfirmOrderUseCase useCase;

    @Test
    void execute_withValidProcessingOrder_shouldConfirmSuccessfully() {
        // Given
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime now = LocalDateTime.now();

        Order processingOrder = new Order(
            orderId, eventId, customerId,
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PROCESSING,
            2,
            0,
            1,
            null,
            now,
            now,
            null
        );

        Order confirmedOrder = processingOrder.confirm();

        Ticket ticket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.PENDING_CONFIRMATION,
            customerId, orderId, now, now.plusMinutes(10),
            1, now, now
        );

        Ticket ticket2 = new Ticket(
            "ticket-2", eventId, TicketStatus.PENDING_CONFIRMATION,
            customerId, orderId, now, now.plusMinutes(10),
            1, now, now
        );

        Ticket ticket1Sold = ticket1.confirmTicketSale();
        Ticket ticket2Sold = ticket2.confirmTicketSale();

        Event event = new Event(
            eventId, "Test Event", LocalDateTime.now().plusDays(1),
            "Description",
            Integer.valueOf(100), Integer.valueOf(48), Integer.valueOf(2), Integer.valueOf(0), Integer.valueOf(1), now, now
        );

        Event updatedEvent = new Event(
            eventId, "Test Event", event.date(),
            "Description", Integer.valueOf(100), Integer.valueOf(48), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(2), now, now
        );

        // When
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(Flux.just(ticket1, ticket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket1Sold, ticket2Sold));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class)))
            .thenReturn(Mono.just(updatedEvent));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(confirmedOrder));

        // Then
        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.orderId()).isEqualTo(orderId);
                assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
            })
            .verifyComplete();

        verify(orderRepository).findById(orderId);
        verify(ticketRepository).findByOrderId(orderId);
        verify(ticketRepository).saveAll(anyList());
        verify(eventRepository).findById(eventId);
        verify(eventRepository).save(any(Event.class));
        verify(orderRepository).save(any(Order.class));
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

        verify(orderRepository, never()).findById(anyString());
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
            null,
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
        verify(ticketRepository, never()).findByOrderId(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void execute_withPendingOrder_shouldReturnInvalidStateTransitionError() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();

        Order pendingOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.PENDING,
            1,
            1,
            1,
            null,
            now,
            now,
            null
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(pendingOrder));

        StepVerifier.create(useCase.execute(orderId))
            .expectError(InvalidStateTransitionException.class)
            .verify();

        verify(orderRepository).findById(orderId);
        verify(ticketRepository, never()).findByOrderId(anyString());
    }

    @Test
    void execute_withNoTickets_shouldReturnError() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();

        Order processingOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            1,
            1,
            null,
            now,
            now,
            null
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(orderId))
            .expectErrorMatches(error ->
                error instanceof IllegalStateException &&
                error.getMessage().contains("has no associated tickets"))
            .verify();

        verify(orderRepository).findById(orderId);
        verify(ticketRepository).findByOrderId(orderId);
    }

    @Test
    void execute_withAllSoldTickets_shouldSkipConfirmation() {
        String orderId = "order-123";
        String eventId = "event-456";
        LocalDateTime now = LocalDateTime.now();

        Order processingOrder = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PROCESSING,
            2,
            0,
            1,
            null,
            now,
            now,
            null
        );

        Order confirmedOrder = processingOrder.confirm();

        Ticket ticket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.SOLD,
            "customer-123", orderId, now, now,
            1, now, now
        );

        Ticket ticket2 = new Ticket(
            "ticket-2", eventId, TicketStatus.SOLD,
            "customer-123", orderId, now, now,
            1, now, now
        );

        Event event = new Event(
            eventId, "Test Event", LocalDateTime.now().plusDays(1),
            "Venue Location",
            Integer.valueOf(100), Integer.valueOf(48), Integer.valueOf(2), Integer.valueOf(0), Integer.valueOf(1), now, now
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(Flux.just(ticket1, ticket2));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class)))
            .thenReturn(Mono.just(event.confirmSale(2)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(confirmedOrder));

        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
            })
            .verifyComplete();

        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    void execute_withInvalidTicketStates_shouldReturnError() {
        String orderId = "order-123";
        LocalDateTime now = LocalDateTime.now();

        Order processingOrder = new Order(
            orderId, "event-456", "customer-123",
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PROCESSING,
            2,
            0,
            1,
            null,
            now,
            now,
            null
        );

        Ticket ticket1 = new Ticket(
            "ticket-1", "event-456", TicketStatus.RESERVED,
            "customer-123", orderId, now, now.plusMinutes(10),
            1, now, now
        );

        Ticket ticket2 = new Ticket(
            "ticket-2", "event-456", TicketStatus.AVAILABLE,
            null, null, null, null,
            1, now, now
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(Flux.just(ticket1, ticket2));

        StepVerifier.create(useCase.execute(orderId))
            .expectError(InvalidStateTransitionException.class)
            .verify();

        verify(orderRepository).findById(orderId);
        verify(ticketRepository).findByOrderId(orderId);
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    void execute_withComplimentaryOrder_shouldConfirmSuccessfully() {
        String orderId = "order-123";
        String eventId = "event-456";
        String customerId = "customer-789";
        LocalDateTime now = LocalDateTime.now();

        Order processingOrder = new Order(
            orderId, eventId, customerId,
            List.of("ticket-1"),
            OrderStatus.PROCESSING,
            1,
            1,
            1,
            null,
            now,
            now,
            null
        );

        Order confirmedOrder = processingOrder.confirm();

        Ticket ticket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.PENDING_CONFIRMATION,
            customerId, orderId, now, now.plusMinutes(10),
            1, now, now
        );

        Ticket ticket1Sold = ticket1.confirmTicketSale();

        Event event = new Event(
            eventId, "Test Event", LocalDateTime.now().plusDays(1),
            "Description",
            Integer.valueOf(100), Integer.valueOf(49), Integer.valueOf(1), Integer.valueOf(0), Integer.valueOf(1), now, now
        );

        Event updatedEvent = new Event(
            eventId, "Test Event", event.date(),
            "Description", Integer.valueOf(100), Integer.valueOf(49), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(2), now, now
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(processingOrder));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(Flux.just(ticket1));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket1Sold));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class)))
            .thenReturn(Mono.just(updatedEvent));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(confirmedOrder));

        StepVerifier.create(useCase.execute(orderId))
            .assertNext(result -> {
                assertThat(result.orderId()).isEqualTo(orderId);
                assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
            })
            .verifyComplete();

        verify(eventRepository).save(any(Event.class));
    }
}

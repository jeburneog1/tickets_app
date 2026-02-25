package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.domain.service.MessageQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCase Tests")
class CreateOrderUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ReserveTicketsUseCase reserveTicketsUseCase;
    @Mock
    private MessageQueueService messageQueueService;
    private CreateOrderUseCase createOrderUseCase;
    @BeforeEach
    void setUp() {
        createOrderUseCase = new CreateOrderUseCase(
            eventRepository,
            orderRepository,
            ticketRepository,
            reserveTicketsUseCase,
            messageQueueService
        );
    }
    @Test
    @DisplayName("Should create order successfully with valid input")
    void shouldCreateOrderSuccessfully() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 2;
        Event event = Event.create(eventId, "Rock Concert", LocalDateTime.now().plusDays(30), "Venue", 1000);
        Ticket ticket1 = Ticket.createAvailableTicket("ticket-1", eventId).reserveTicket(customerId, null);
        Ticket ticket2 = Ticket.createAvailableTicket("ticket-2", eventId).reserveTicket(customerId, null);
        List<Ticket> reservedTickets = List.of(ticket1, ticket2);
        Order pendingOrder = Order.createPending("order-123", eventId, customerId, List.of("ticket-1", "ticket-2"));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(reservedTickets));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(pendingOrder));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(reservedTickets));
        when(messageQueueService.sendOrderForProcessing(anyString())).thenReturn(Mono.empty());
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertNotNull(order);
                assertEquals(OrderStatus.PENDING, order.status());
                assertEquals(eventId, order.eventId());
                assertEquals(customerId, order.customerId());
                assertEquals(2, order.totalTickets());
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId);
        verify(reserveTicketsUseCase, times(1)).execute(eq(eventId), eq(customerId), eq(quantity), anyString());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(ticketRepository, times(1)).saveAll(anyList());
        verify(messageQueueService, times(1)).sendOrderForProcessing(anyString());
    }
    @Test
    @DisplayName("Should throw EventNotFoundException when event does not exist")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        String eventId = "non-existent-event";
        String customerId = "customer-789";
        Integer quantity = 2;
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .expectError(EventNotFoundException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoInteractions(reserveTicketsUseCase);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(messageQueueService);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        Mono<Order> result = createOrderUseCase.execute(invalidEventId, "customer-789", 2);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(reserveTicketsUseCase);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(messageQueueService);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when customer ID is null or blank")
    void shouldThrowExceptionWhenCustomerIdIsNullOrBlank(String invalidCustomerId) {
        Mono<Order> result = createOrderUseCase.execute("event-123", invalidCustomerId, 2);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Customer ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(reserveTicketsUseCase);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(messageQueueService);
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Should throw exception when quantity is zero or negative")
    void shouldThrowExceptionWhenQuantityIsZeroOrNegative(Integer invalidQuantity) {
        Mono<Order> result = createOrderUseCase.execute("event-123", "customer-789", invalidQuantity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Quantity must be positive"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(reserveTicketsUseCase);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(messageQueueService);
    }
    @Test
    @DisplayName("Should throw exception when quantity is null")
    void shouldThrowExceptionWhenQuantityIsNull() {
        Mono<Order> result = createOrderUseCase.execute("event-123", "customer-789", null);
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(eventRepository);
    }
    @Test
    @DisplayName("Should handle reservation failure")
    void shouldHandleReservationFailure() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 2;
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 1000);
        RuntimeException reservationError = new RuntimeException("Insufficient tickets");
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.error(reservationError));
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verify(reserveTicketsUseCase, times(1)).execute(eq(eventId), eq(customerId), eq(quantity), anyString());
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(messageQueueService);
    }
    @Test
    @DisplayName("Should handle order repository save failure")
    void shouldHandleOrderRepositorySaveFailure() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 1;
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 1000);
        Ticket ticket = Ticket.createAvailableTicket("ticket-1", eventId).reserveTicket(customerId, null);
        RuntimeException saveError = new RuntimeException("Database error");
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(List.of(ticket)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.error(saveError));
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(messageQueueService);
    }
    @Test
    @DisplayName("Should handle message queue service failure")
    void shouldHandleMessageQueueServiceFailure() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 1;
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 1000);
        Ticket ticket = Ticket.createAvailableTicket("ticket-1", eventId).reserveTicket(customerId, null);
        Order pendingOrder = Order.createPending("order-123", eventId, customerId, List.of("ticket-1"));
        RuntimeException queueError = new RuntimeException("Queue unavailable");
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(List.of(ticket)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(pendingOrder));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket));
        when(messageQueueService.sendOrderForProcessing(anyString())).thenReturn(Mono.error(queueError));
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(messageQueueService, times(1)).sendOrderForProcessing(anyString());
    }
    @Test
    @DisplayName("Should create order with single ticket")
    void shouldCreateOrderWithSingleTicket() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 1;
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 100);
        Ticket ticket = Ticket.createAvailableTicket("ticket-1", eventId).reserveTicket(customerId, null);
        Order pendingOrder = Order.createPending("order-123", eventId, customerId, List.of("ticket-1"));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(List.of(ticket)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(pendingOrder));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket));
        when(messageQueueService.sendOrderForProcessing(anyString())).thenReturn(Mono.empty());
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(1, order.totalTickets());
                assertEquals(OrderStatus.PENDING, order.status());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should create order with maximum tickets")
    void shouldCreateOrderWithMaximumTickets() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = Order.MAX_TICKETS_PER_ORDER;
        Event event = Event.create(eventId, "Big Concert", LocalDateTime.now().plusDays(30), "Stadium", 10000);
        List<Ticket> tickets = generateReservedTickets(eventId, customerId, quantity);
        List<String> ticketIds = tickets.stream().map(Ticket::ticketId).toList();
        Order pendingOrder = Order.createPending("order-123", eventId, customerId, ticketIds);
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(tickets));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(pendingOrder));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(tickets));
        when(messageQueueService.sendOrderForProcessing(anyString())).thenReturn(Mono.empty());
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(Order.MAX_TICKETS_PER_ORDER, order.totalTickets());
                assertEquals(OrderStatus.PENDING, order.status());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should initialize order with correct initial values")
    void shouldInitializeOrderWithCorrectInitialValues() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 2;
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 1000);
        List<Ticket> tickets = generateReservedTickets(eventId, customerId, quantity);
        List<String> ticketIds = tickets.stream().map(Ticket::ticketId).toList();
        Order pendingOrder = Order.createPending("order-123", eventId, customerId, ticketIds);
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(reserveTicketsUseCase.execute(eq(eventId), eq(customerId), eq(quantity), anyString())).thenReturn(Mono.just(tickets));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(pendingOrder));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(tickets));
        when(messageQueueService.sendOrderForProcessing(anyString())).thenReturn(Mono.empty());
        Mono<Order> result = createOrderUseCase.execute(eventId, customerId, quantity);
        StepVerifier.create(result)
            .assertNext(order -> {
                assertEquals(OrderStatus.PENDING, order.status());
                assertEquals(0, order.retryCount());
                assertEquals(0, order.version());
                assertNull(order.processedAt());
                assertNotNull(order.createdAt());
                assertNotNull(order.updatedAt());
            })
            .verifyComplete();
    }
    private List<Ticket> generateReservedTickets(String eventId, String customerId, int count) {
        List<Ticket> tickets = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Ticket available = Ticket.createAvailableTicket("ticket-" + i, eventId);
            tickets.add(available.reserveTicket(customerId, null));
        }
        return tickets;
    }
}
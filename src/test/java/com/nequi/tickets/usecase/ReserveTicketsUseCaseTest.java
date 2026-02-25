package com.nequi.tickets.usecase;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.exception.MaxTicketsExceededException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveTicketsUseCase Tests")
class ReserveTicketsUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private BusinessProperties businessProperties;
    
    private ReserveTicketsUseCase reserveTicketsUseCase;
    
    @BeforeEach
    void setUp() {
        BusinessProperties.Order order = new BusinessProperties.Order();
        order.setMaxTicketsPerOrder(10);
        lenient().when(businessProperties.getOrder()).thenReturn(order);
        
        reserveTicketsUseCase = new ReserveTicketsUseCase(eventRepository, ticketRepository, businessProperties);
    }
    @Test
    @DisplayName("Should reserve tickets successfully")
    void shouldReserveTicketsSuccessfully() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 2;
        String orderId = "order-456";
        Event event = new Event(
            eventId, "Rock Concert", LocalDateTime.now().plusDays(30),
            "Madison Square Garden", Integer.valueOf(1000), Integer.valueOf(800), Integer.valueOf(200), Integer.valueOf(0), Integer.valueOf(0),
            LocalDateTime.now(), LocalDateTime.now()
        );
        Event updatedEvent = new Event(
            eventId, event.name(), event.date(), event.location(),
            event.totalCapacity(), Integer.valueOf(798), Integer.valueOf(202), Integer.valueOf(0), Integer.valueOf(1),
            event.createdAt(), LocalDateTime.now()
        );
        List<Ticket> availableTickets = List.of(
            Ticket.createAvailableTicket("ticket-1", eventId),
            Ticket.createAvailableTicket("ticket-2", eventId)
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(updatedEvent));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .assertNext(tickets -> {
                assertEquals(2, tickets.size());
                tickets.forEach(ticket -> {
                    assertEquals(TicketStatus.RESERVED, ticket.status());
                    assertEquals(customerId, ticket.customerId());
                    assertEquals(orderId, ticket.orderId());
                    assertNotNull(ticket.reservedAt());
                    assertNotNull(ticket.reservationExpiresAt());
                });
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(ticketRepository, times(1)).findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
        verify(ticketRepository, times(1)).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw EventNotFoundException when event does not exist")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        String eventId = "non-existent-event";
        String customerId = "customer-789";
        Integer quantity = 2;
        String orderId = "order-456";
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .expectError(EventNotFoundException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should throw InsufficientTicketsException when not enough tickets available")
    void shouldThrowInsufficientTicketsExceptionWhenNotEnoughTickets() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 5;
        String orderId = "order-456";
        Event event = new Event(
            eventId, "Popular Concert", LocalDateTime.now().plusDays(30),
            "Venue", Integer.valueOf(1000), Integer.valueOf(2), Integer.valueOf(998), Integer.valueOf(0), Integer.valueOf(0),  
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .expectError(InsufficientTicketsException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, never()).save(any(Event.class));
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should throw MaxTicketsExceededException when quantity exceeds maximum")
    void shouldThrowMaxTicketsExceededExceptionWhenQuantityExceedsMaximum() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = Order.MAX_TICKETS_PER_ORDER + 1;
        String orderId = "order-456";
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .expectError(MaxTicketsExceededException.class)
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(invalidEventId, "customer-789", 2, "order-456");
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when customer ID is null or blank")
    void shouldThrowExceptionWhenCustomerIdIsNullOrBlank(String invalidCustomerId) {
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute("event-123", invalidCustomerId, 2, "order-456");
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Customer ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Should throw exception when quantity is zero or negative")
    void shouldThrowExceptionWhenQuantityIsZeroOrNegative(Integer invalidQuantity) {
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute("event-123", "customer-789", invalidQuantity, "order-456");
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Quantity must be positive"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should throw exception when quantity is null")
    void shouldThrowExceptionWhenQuantityIsNull() {
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute("event-123", "customer-789", null, "order-456");
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when order ID is null or blank")
    void shouldThrowExceptionWhenOrderIdIsNullOrBlank(String invalidOrderId) {
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute("event-123", "customer-789", 2, invalidOrderId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Order ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should reserve single ticket successfully")
    void shouldReserveSingleTicketSuccessfully() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 1;
        String orderId = "order-456";
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 100);
        Event updatedEvent = new Event(
            eventId, event.name(), event.date(), event.location(),
            event.totalCapacity(), Integer.valueOf(99), Integer.valueOf(1), Integer.valueOf(0), Integer.valueOf(1),
            event.createdAt(), LocalDateTime.now()
        );
        List<Ticket> availableTickets = List.of(
            Ticket.createAvailableTicket("ticket-1", eventId)
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(updatedEvent));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .assertNext(tickets -> {
                assertEquals(1, tickets.size());
                assertEquals(TicketStatus.RESERVED, tickets.get(0).status());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should reserve maximum allowed tickets successfully")
    void shouldReserveMaximumAllowedTicketsSuccessfully() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = Order.MAX_TICKETS_PER_ORDER;
        String orderId = "order-456";
        Event event = Event.create(eventId, "Big Concert", LocalDateTime.now().plusDays(30), "Stadium", 10000);
        Event updatedEvent = new Event(
            eventId, event.name(), event.date(), event.location(),
            event.totalCapacity(), event.availableTickets() - quantity, event.reservedTickets() + quantity, Integer.valueOf(0), Integer.valueOf(1),
            event.createdAt(), LocalDateTime.now()
        );
        List<Ticket> availableTickets = java.util.stream.IntStream.range(0, quantity)
            .mapToObj(i -> Ticket.createAvailableTicket("ticket-" + i, eventId))
            .toList();
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(updatedEvent));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .assertNext(tickets -> {
                assertEquals(Order.MAX_TICKETS_PER_ORDER, tickets.size());
                tickets.forEach(ticket -> assertEquals(TicketStatus.RESERVED, ticket.status()));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should set reservation expiration time correctly")
    void shouldSetReservationExpirationTimeCorrectly() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 1;
        String orderId = "order-456";
        Event event = Event.create(eventId, "Concert", LocalDateTime.now().plusDays(30), "Venue", 100);
        Event updatedEvent = new Event(
            eventId, event.name(), event.date(), event.location(),
            event.totalCapacity(), event.availableTickets() - 1, event.reservedTickets() + 1, Integer.valueOf(0), Integer.valueOf(1),
            event.createdAt(), LocalDateTime.now()
        );
        List<Ticket> availableTickets = List.of(
            Ticket.createAvailableTicket("ticket-1", eventId)
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(updatedEvent));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .assertNext(tickets -> {
                Ticket firstTicket = tickets.get(0);
                assertNotNull(firstTicket.reservedAt());
                assertNotNull(firstTicket.reservationExpiresAt());
                assertTrue(firstTicket.reservationExpiresAt().isAfter(firstTicket.reservedAt()));
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle repository error gracefully")
    void shouldHandleRepositoryErrorGracefully() {
        String eventId = "event-123";
        String customerId = "customer-789";
        Integer quantity = 2;
        String orderId = "order-456";
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(eventRepository.findById(eventId)).thenReturn(Mono.error(repositoryError));
        Mono<List<Ticket>> result = reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }
}
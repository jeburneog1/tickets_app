package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignComplimentaryTicketUseCase Tests")
class AssignComplimentaryTicketUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderRepository orderRepository;

    private AssignComplimentaryTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AssignComplimentaryTicketUseCase(eventRepository, ticketRepository, orderRepository);
    }

    @Test
    @DisplayName("Should assign complimentary ticket successfully")
    void shouldAssignComplimentaryTicketSuccessfully() {
        // Given
        String eventId = "event-123";
        String customerId = "customer-456";
        String reason = "VIP guest";
        
        Event event = Event.create(
            eventId, 
            "Concert", 
            LocalDateTime.now().plusDays(30), 
            "Arena", 
            1000);

        Ticket availableTicket = new Ticket(
            "ticket-789",
            eventId,
            TicketStatus.AVAILABLE,
            null,
            null,
            null,
            null,
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        Order savedOrder = Order.createComplimentary(
            "order-123",
            eventId,
            customerId,
            java.util.List.of("ticket-789")
        );

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.just(availableTicket));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            return Mono.just(ticket);
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            return Mono.just(e);
        });

        // When
        Mono<Ticket> result = useCase.execute(eventId, customerId, reason);

        // Then
        StepVerifier.create(result)
            .assertNext(ticket -> {
                assertNotNull(ticket);
                assertEquals("ticket-789", ticket.ticketId());
                assertEquals(eventId, ticket.eventId());
                assertEquals(TicketStatus.COMPLIMENTARY, ticket.status());
            })
            .verifyComplete();

        // Verify interactions
        verify(eventRepository).findById(eventId);
        verify(ticketRepository).findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
        verify(orderRepository).save(argThat(order -> 
            order.eventId().equals(eventId) &&
            order.customerId().equals(customerId) &&
            order.status() == OrderStatus.CONFIRMED
        ));
        verify(ticketRepository).save(argThat(ticket -> 
            ticket.ticketId().equals("ticket-789") &&
            ticket.status() == TicketStatus.COMPLIMENTARY
        ));
        
        // Capture and verify the event that was saved
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();
        assertEquals(eventId, savedEvent.eventId());
        assertEquals(999, savedEvent.availableTickets());
        assertEquals(1, savedEvent.complimentaryTickets());
    }

    @Test
    @DisplayName("Should fail when event not found")
    void shouldFailWhenEventNotFound() {
        // Given
        String eventId = "non-existent";
        String customerId = "customer-456";
        String reason = "VIP guest";

        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());

        // When
        Mono<Ticket> result = useCase.execute(eventId, customerId, reason);

        // Then
        StepVerifier.create(result)
            .expectError(EventNotFoundException.class)
            .verify();

        verify(eventRepository).findById(eventId);
        verifyNoInteractions(ticketRepository, orderRepository);
    }

    @Test
    @DisplayName("Should fail when no available tickets")
    void shouldFailWhenNoAvailableTickets() {
        // Given
        String eventId = "event-123";
        String customerId = "customer-456";
        String reason = "VIP guest";
        
        Event event = Event.create(
            eventId, 
            "Concert", 
            LocalDateTime.now().plusDays(30), 
            "Arena", 
            1000);

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE))
            .thenReturn(Flux.empty());

        // When
        Mono<Ticket> result = useCase.execute(eventId, customerId, reason);

        // Then
        StepVerifier.create(result)
            .expectError(InsufficientTicketsException.class)
            .verify();

        verify(eventRepository, atLeast(1)).findById(eventId);
        verify(ticketRepository, atLeast(1)).findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Should validate required parameters")
    void shouldValidateRequiredParameters() {
        // Test null eventId
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(null, "customer-123", "reason").block();
        });

        // Test blank eventId
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute("", "customer-123", "reason").block();
        });

        // Test null customerId
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute("event-123", null, "reason").block();
        });

        // Test blank customerId
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute("event-123", "", "reason").block();
        });

        // Test null reason
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute("event-123", "customer-123", null).block();
        });

        // Test blank reason
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute("event-123", "customer-123", "").block();
        });
    }
}

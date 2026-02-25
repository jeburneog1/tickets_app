package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ReleaseExpiredReservationsUseCaseTest {
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private ReleaseExpiredReservationsUseCase useCase;
    @Test
    void execute_withNoExpiredReservations_shouldReturnZero() {
        LocalDateTime now = LocalDateTime.now();
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class))).thenReturn(Flux.empty());
        StepVerifier.create(useCase.execute())
            .expectNext(0)
            .verifyComplete();
        verify(ticketRepository).findExpiredReservations(any(LocalDateTime.class));
        verify(ticketRepository, never()).saveAll(anyList());
        verify(eventRepository, never()).updateInventory(anyString(), anyInt(), anyInt(), anyInt());
    }
    @Test
    void execute_withExpiredReservations_shouldReleaseAndReturnCount() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = "event-123";
        String orderId = "order-456";
        Ticket expiredTicket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Ticket expiredTicket2 = new Ticket(
            "ticket-2", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Event event = Event.create(
            eventId, "Concert",
            now.plusDays(30), "Venue", 100
        );
        Order order = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1", "ticket-2"),
            OrderStatus.PENDING,
            2,
            0,
            1,
            null,
            now,
            now,
            null
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(event));
        when(orderRepository.findByIds(anyList())).thenReturn(Flux.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        StepVerifier.create(useCase.execute())
            .expectNext(2)
            .verifyComplete();
        verify(ticketRepository).findExpiredReservations(any(LocalDateTime.class));
        verify(ticketRepository).saveAll(anyList());
        verify(eventRepository).updateInventory(anyString(), anyInt(), anyInt(), anyInt());
    }
    @Test
    void execute_shouldTransformTicketsToAvailable() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = "event-123";
        Ticket expiredTicket = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", "order-456", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Event event = Event.create(
            eventId, "Concert",
            now.plusDays(30), "Venue", 100
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class))).thenReturn(Flux.just(expiredTicket));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(event));
        when(orderRepository.findByIds(anyList())).thenReturn(Flux.empty());
        StepVerifier.create(useCase.execute())
            .expectNext(1)
            .verifyComplete();
        ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(ticketCaptor.capture());
        List<Ticket> savedTickets = ticketCaptor.getValue();
        assertThat(savedTickets).hasSize(1);
        Ticket releasedTicket = savedTickets.get(0);
        assertThat(releasedTicket.status()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(releasedTicket.customerId()).isNull();
        assertThat(releasedTicket.orderId()).isNull();
        assertThat(releasedTicket.reservedAt()).isNull();
        assertThat(releasedTicket.reservationExpiresAt()).isNull();
        assertThat(releasedTicket.version()).isEqualTo(2); 
    }
    @Test
    void execute_withMultipleEvents_shouldUpdateAllInventories() {
        LocalDateTime now = LocalDateTime.now();
        String event1Id = "event-1";
        String event2Id = "event-2";
        Ticket expiredTicket1 = new Ticket(
            "ticket-1", event1Id, TicketStatus.RESERVED,
            "customer-123", "order-1", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Ticket expiredTicket2 = new Ticket(
            "ticket-2", event1Id, TicketStatus.RESERVED,
            "customer-123", "order-1", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Ticket expiredTicket3 = new Ticket(
            "ticket-3", event2Id, TicketStatus.RESERVED,
            "customer-456", "order-2", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Event event1 = new Event(
            event1Id, "Concert 1", now.plusDays(30),
            "Venue 1", Integer.valueOf(100), Integer.valueOf(95), Integer.valueOf(5),
            Integer.valueOf(0), Integer.valueOf(1), now, now
        );
        Event event2 = new Event(
            event2Id, "Concert 2", now.plusDays(30),
            "Venue 2", Integer.valueOf(50), Integer.valueOf(48), Integer.valueOf(2),
            Integer.valueOf(0), Integer.valueOf(1), now, now
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Flux.just(expiredTicket1, expiredTicket2, expiredTicket3));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket1, expiredTicket2, expiredTicket3));
        when(eventRepository.findById(event1Id)).thenReturn(Mono.just(event1));
        when(eventRepository.findById(event2Id)).thenReturn(Mono.just(event2));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt()))
            .thenReturn(Mono.just(event1), Mono.just(event2));
        when(orderRepository.findByIds(anyList())).thenReturn(Flux.empty());
        StepVerifier.create(useCase.execute())
            .expectNext(3)
            .verifyComplete();
        verify(eventRepository).updateInventory(eq(event1Id), anyInt(), eq(97), eq(3)); 
        verify(eventRepository).updateInventory(eq(event2Id), anyInt(), eq(49), eq(1)); 
    }
    @Test
    void execute_shouldCancelAffectedOrders() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = "event-123";
        String order1Id = "order-1";
        String order2Id = "order-2";
        Ticket expiredTicket1 = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", order1Id, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Ticket expiredTicket2 = new Ticket(
            "ticket-2", eventId, TicketStatus.RESERVED,
            "customer-456", order2Id, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Order order1 = new Order(
            order1Id, eventId, "customer-123",
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
        Order order2 = new Order(
            order2Id, eventId, "customer-456",
            List.of("ticket-2"),
            OrderStatus.PROCESSING,
            1,
            1,
            1,
            null,
            now,
            now,
            null
        );
        Event event = Event.create(
            eventId, "Concert",
            now.plusDays(30), "Venue", 100
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(event));
        when(orderRepository.findByIds(List.of(order1Id, order2Id))).thenReturn(Flux.just(order1, order2));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order1), Mono.just(order2));
        StepVerifier.create(useCase.execute())
            .expectNext(2)
            .verifyComplete();
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        List<Order> savedOrders = orderCaptor.getAllValues();
        assertThat(savedOrders).allMatch(order -> order.status() == OrderStatus.CANCELLED);
    }
    @Test
    void execute_shouldNotCancelAlreadyConfirmedOrders() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = "event-123";
        String orderId = "order-1";
        Ticket expiredTicket = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Order confirmedOrder = new Order(
            orderId, eventId, "customer-123",
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
        Event event = Event.create(
            eventId, "Concert",
            now.plusDays(30), "Venue", 100
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class))).thenReturn(Flux.just(expiredTicket));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(event));
        when(orderRepository.findByIds(List.of(orderId))).thenReturn(Flux.just(confirmedOrder));
        StepVerifier.create(useCase.execute())
            .expectNext(1)
            .verifyComplete();
        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void execute_shouldNotCancelAlreadyCancelledOrders() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = "event-123";
        String orderId = "order-1";
        Ticket expiredTicket = new Ticket(
            "ticket-1", eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Order cancelledOrder = new Order(
            orderId, eventId, "customer-123",
            List.of("ticket-1"),
            OrderStatus.CANCELLED,
            1,
            1,
            1,
            "Reservation expired",
            now,
            now,
            null
        );
        Event event = Event.create(
            eventId, "Concert",
            now.plusDays(30), "Venue", 100
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class))).thenReturn(Flux.just(expiredTicket));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(event));
        when(orderRepository.findByIds(List.of(orderId))).thenReturn(Flux.just(cancelledOrder));
        StepVerifier.create(useCase.execute())
            .expectNext(1)
            .verifyComplete();
        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void execute_withEventUpdateError_shouldContinueDelayingError() {
        LocalDateTime now = LocalDateTime.now();
        String event1Id = "event-1";
        String event2Id = "event-2";
        Ticket expiredTicket1 = new Ticket(
            "ticket-1", event1Id, TicketStatus.RESERVED,
            "customer-123", "order-1", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Ticket expiredTicket2 = new Ticket(
            "ticket-2", event2Id, TicketStatus.RESERVED,
            "customer-456", "order-2", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        Event event2 = new Event(
            event2Id, "Concert 2",
            now.plusDays(30), "Venue", Integer.valueOf(100),
            Integer.valueOf(99),  
            Integer.valueOf(1),
            Integer.valueOf(0),
            Integer.valueOf(0),   
            now, now
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(expiredTicket1, expiredTicket2));
        when(eventRepository.findById(event1Id)).thenReturn(Mono.error(new RuntimeException("Event 1 not found")));
        when(eventRepository.findById(event2Id)).thenReturn(Mono.just(event2));
        when(eventRepository.updateInventory(event2Id, event2.version(), event2.availableTickets() + 1, event2.reservedTickets() - 1))
            .thenReturn(Mono.just(event2));
        when(orderRepository.findByIds(anyList())).thenReturn(Flux.empty());
        StepVerifier.create(useCase.execute())
            .expectNext(2)
            .verifyComplete();
        verify(eventRepository).findById(event1Id);
        verify(eventRepository).findById(event2Id);
    }
    @Test
    void execute_withTicketSaveError_shouldPropagateError() {
        LocalDateTime now = LocalDateTime.now();
        Ticket expiredTicket = new Ticket(
            "ticket-1", "event-123", TicketStatus.RESERVED,
            "customer-123", "order-456", now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class))).thenReturn(Flux.just(expiredTicket));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.error(new RuntimeException("Save failed")));
        StepVerifier.create(useCase.execute())
            .expectError(RuntimeException.class)
            .verify();
    }
    @Test
    void execute_shouldGroupTicketsByEvent() {
        LocalDateTime now = LocalDateTime.now();
        String event1Id = "event-1";
        String event2Id = "event-2";
        List<Ticket> event1Tickets = List.of(
            createExpiredTicket("t1", event1Id, "o1", now),
            createExpiredTicket("t2", event1Id, "o1", now),
            createExpiredTicket("t3", event1Id, "o2", now),
            createExpiredTicket("t4", event1Id, "o2", now),
            createExpiredTicket("t5", event1Id, "o3", now)
        );
        List<Ticket> event2Tickets = List.of(
            createExpiredTicket("t6", event2Id, "o4", now),
            createExpiredTicket("t7", event2Id, "o4", now),
            createExpiredTicket("t8", event2Id, "o5", now)
        );
        Event event1 = new Event(
            event1Id, "Event 1", now.plusDays(30),
            "Venue", Integer.valueOf(100), Integer.valueOf(90), Integer.valueOf(10),
            Integer.valueOf(0), Integer.valueOf(5), now, now
        );
        Event event2 = new Event(
            event2Id, "Event 2", now.plusDays(30),
            "Venue", Integer.valueOf(50), Integer.valueOf(45), Integer.valueOf(5),
            Integer.valueOf(0), Integer.valueOf(3), now, now
        );
        when(ticketRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Flux.fromIterable(event1Tickets).concatWith(Flux.fromIterable(event2Tickets)));
        when(ticketRepository.saveAll(anyList())).thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(eventRepository.findById(event1Id)).thenReturn(Mono.just(event1));
        when(eventRepository.findById(event2Id)).thenReturn(Mono.just(event2));
        when(eventRepository.updateInventory(anyString(), anyInt(), anyInt(), anyInt()))
            .thenReturn(Mono.just(event1), Mono.just(event2));
        when(orderRepository.findByIds(anyList())).thenReturn(Flux.empty());
        StepVerifier.create(useCase.execute())
            .expectNext(8)
            .verifyComplete();
        verify(eventRepository).updateInventory(eq(event1Id), eq(5), eq(95), eq(5)); 
        verify(eventRepository).updateInventory(eq(event2Id), eq(3), eq(48), eq(2)); 
    }
    private Ticket createExpiredTicket(String ticketId, String eventId, String orderId, LocalDateTime now) {
        return new Ticket(
            ticketId, eventId, TicketStatus.RESERVED,
            "customer-123", orderId, now.minusMinutes(20), now.minusMinutes(10),
            1, now, now
        );
    }
}
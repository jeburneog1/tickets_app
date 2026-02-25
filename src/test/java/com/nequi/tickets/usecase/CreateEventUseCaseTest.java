package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateEventUseCase Tests")
class CreateEventUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketRepository ticketRepository;
    private CreateEventUseCase createEventUseCase;
    @BeforeEach
    void setUp() {
        createEventUseCase = new CreateEventUseCase(eventRepository, ticketRepository);
    }
    @Test
    @DisplayName("Should create event successfully with valid input")
    void shouldCreateEventSuccessfully() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Event expectedEvent = Event.create("event-123", name, date, location, totalCapacity);
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(expectedEvent));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<Event> result = createEventUseCase.execute(name, date, location, totalCapacity);
        StepVerifier.create(result)
            .assertNext(event -> {
                assertNotNull(event);
                assertEquals(name, event.name());
                assertEquals(date, event.date());
                assertEquals(location, event.location());
                assertEquals(totalCapacity, event.totalCapacity());
                assertEquals(totalCapacity, event.availableTickets());
                assertEquals(0, event.reservedTickets());
                assertEquals(0, event.version());
            })
            .verifyComplete();
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(ticketRepository, times(1)).saveAll(anyList());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when event name is null or blank")
    void shouldThrowExceptionWhenNameIsNullOrBlank(String invalidName) {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Mono<Event> result = createEventUseCase.execute(invalidName, date, location, totalCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event name is required"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw exception when event date is null")
    void shouldThrowExceptionWhenDateIsNull() {
        String name = "Rock Concert";
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Mono<Event> result = createEventUseCase.execute(name, null, location, totalCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event date is required"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw exception when event date is in the past")
    void shouldThrowExceptionWhenDateIsInThePast() {
        String name = "Rock Concert";
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Mono<Event> result = createEventUseCase.execute(name, pastDate, location, totalCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event date must be in the future"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when location is null or blank")
    void shouldThrowExceptionWhenLocationIsNullOrBlank(String invalidLocation) {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        Integer totalCapacity = 1000;
        Mono<Event> result = createEventUseCase.execute(name, date, invalidLocation, totalCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event location is required"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw exception when total capacity is null")
    void shouldThrowExceptionWhenTotalCapacityIsNull() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Mono<Event> result = createEventUseCase.execute(name, date, location, null);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Total capacity must be positive"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw exception when total capacity is zero")
    void shouldThrowExceptionWhenTotalCapacityIsZero() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer zeroCapacity = 0;
        Mono<Event> result = createEventUseCase.execute(name, date, location, zeroCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Total capacity must be positive"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should throw exception when total capacity is negative")
    void shouldThrowExceptionWhenTotalCapacityIsNegative() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer negativeCapacity = -100;
        Mono<Event> result = createEventUseCase.execute(name, date, location, negativeCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Total capacity must be positive"))
            .verify();
        verify(eventRepository, never()).save(any(Event.class));
        verify(ticketRepository, never()).saveAll(anyList());
    }
    @Test
    @DisplayName("Should initialize event with all tickets available")
    void shouldInitializeEventWithAllTicketsAvailable() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Event savedEvent = Event.create("event-123", name, date, location, totalCapacity);
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(savedEvent));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<Event> result = createEventUseCase.execute(name, date, location, totalCapacity);
        StepVerifier.create(result)
            .assertNext(event -> {
                assertEquals(totalCapacity, event.availableTickets());
                assertEquals(0, event.reservedTickets());
            })
            .verifyComplete();
        verify(ticketRepository, times(1)).saveAll(anyList());
    }
    @Test
    @DisplayName("Should initialize event with version 0")
    void shouldInitializeEventWithVersionZero() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Event savedEvent = Event.create("event-123", name, date, location, totalCapacity);
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(savedEvent));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> tickets = invocation.getArgument(0);
            return Flux.fromIterable(tickets);
        });
        Mono<Event> result = createEventUseCase.execute(name, date, location, totalCapacity);
        StepVerifier.create(result)
            .assertNext(event -> assertEquals(0, event.version()))
            .verifyComplete();
        verify(ticketRepository, times(1)).saveAll(anyList());
    }
    @Test
    @DisplayName("Should handle repository error")
    void shouldHandleRepositoryError() {
        String name = "Rock Concert";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.error(repositoryError));
        Mono<Event> result = createEventUseCase.execute(name, date, location, totalCapacity);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof RuntimeException &&
                throwable.getMessage().equals("Database connection failed"))
            .verify();
    }
    @Test
    @DisplayName("Should not call repository save when validation fails")
    void shouldNotCallRepositorySaveWhenValidationFails() {
        String invalidName = "";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Madison Square Garden";
        Integer totalCapacity = 1000;
        Mono<Event> result = createEventUseCase.execute(invalidName, date, location, totalCapacity);
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
}
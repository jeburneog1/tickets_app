package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.TicketStatus;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckAvailabilityUseCase Tests")
class CheckAvailabilityUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketRepository ticketRepository;
    private CheckAvailabilityUseCase checkAvailabilityUseCase;
    @BeforeEach
    void setUp() {
        checkAvailabilityUseCase = new CheckAvailabilityUseCase(eventRepository, ticketRepository);
    }
    @Test
    @DisplayName("Should check availability successfully when event exists")
    void shouldCheckAvailabilitySuccessfully() {
        String eventId = "event-123";
        Event event = Event.create(eventId, "Rock Concert", LocalDateTime.now().plusDays(30), "Madison Square Garden", 1000);
        Long availableCount = 750L;
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.just(availableCount));
        Mono<Integer> result = checkAvailabilityUseCase.execute(eventId);
        StepVerifier.create(result)
            .assertNext(count -> assertEquals(750, count))
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId);
        verify(ticketRepository, times(1)).countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
    }
    @Test
    @DisplayName("Should return zero when no tickets available")
    void shouldReturnZeroWhenNoTicketsAvailable() {
        String eventId = "event-123";
        Event event = Event.create(eventId, "Sold Out Concert", LocalDateTime.now().plusDays(30), "Madison Square Garden", 1000);
        Long availableCount = 0L;
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.just(availableCount));
        Mono<Integer> result = checkAvailabilityUseCase.execute(eventId);
        StepVerifier.create(result)
            .assertNext(count -> assertEquals(0, count))
            .verifyComplete();
    }
    @Test
    @DisplayName("Should throw EventNotFoundException when event does not exist")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        String eventId = "non-existent-event";
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());
        Mono<Integer> result = checkAvailabilityUseCase.execute(eventId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof EventNotFoundException &&
                throwable.getMessage().contains("Event not found with ID: " + eventId))
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw IllegalArgumentException when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        Mono<Integer> result = checkAvailabilityUseCase.execute(invalidEventId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should handle repository error from event repository")
    void shouldHandleEventRepositoryError() {
        String eventId = "event-123";
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(eventRepository.findById(eventId)).thenReturn(Mono.error(repositoryError));
        Mono<Integer> result = checkAvailabilityUseCase.execute(eventId);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
    }
    @Test
    @DisplayName("Should handle repository error from ticket repository")
    void shouldHandleTicketRepositoryError() {
        String eventId = "event-123";
        Event event = Event.create(eventId, "Rock Concert", LocalDateTime.now().plusDays(30), "Madison Square Garden", 1000);
        RuntimeException repositoryError = new RuntimeException("Query failed");
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.error(repositoryError));
        Mono<Integer> result = checkAvailabilityUseCase.execute(eventId);
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }
    @Test
    @DisplayName("Should get detailed availability information")
    void shouldGetDetailedAvailabilityInformation() {
        String eventId = "event-123";
        Event event = new Event(
            eventId, "Rock Concert", LocalDateTime.now().plusDays(30),
            "Madison Square Garden", Integer.valueOf(1000), Integer.valueOf(600), Integer.valueOf(300), Integer.valueOf(0), Integer.valueOf(0),
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.just(600L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.RESERVED)).thenReturn(Mono.just(300L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.SOLD)).thenReturn(Mono.just(100L));
        Mono<CheckAvailabilityUseCase.AvailabilityDetails> result = checkAvailabilityUseCase.executeDetailed(eventId);
        StepVerifier.create(result)
            .assertNext(details -> {
                assertEquals(1000, details.totalCapacity());
                assertEquals(600, details.availableTickets());
                assertEquals(300, details.reservedTickets());
                assertEquals(100, details.soldTickets());
                assertNotNull(details.checkedAt());
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId);
        verify(ticketRepository, times(1)).countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
        verify(ticketRepository, times(1)).countByEventIdAndStatus(eventId, TicketStatus.RESERVED);
        verify(ticketRepository, times(1)).countByEventIdAndStatus(eventId, TicketStatus.SOLD);
    }
    @Test
    @DisplayName("Should throw EventNotFoundException when getting detailed availability for non-existent event")
    void shouldThrowExceptionWhenGettingDetailedAvailabilityForNonExistentEvent() {
        String eventId = "non-existent-event";
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());
        Mono<CheckAvailabilityUseCase.AvailabilityDetails> result = checkAvailabilityUseCase.executeDetailed(eventId);
        StepVerifier.create(result)
            .expectError(EventNotFoundException.class)
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoInteractions(ticketRepository);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw exception when getting detailed availability with invalid event ID")
    void shouldThrowExceptionWhenGettingDetailedAvailabilityWithInvalidEventId(String invalidEventId) {
        Mono<CheckAvailabilityUseCase.AvailabilityDetails> result = checkAvailabilityUseCase.executeDetailed(invalidEventId);
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(ticketRepository);
    }
    @Test
    @DisplayName("Should handle all available tickets scenario")
    void shouldHandleAllAvailableTicketsScenario() {
        String eventId = "event-123";
        Event event = Event.create(eventId, "New Event", LocalDateTime.now().plusDays(30), "Venue", 1000);
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.just(1000L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.RESERVED)).thenReturn(Mono.just(0L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.SOLD)).thenReturn(Mono.just(0L));
        Mono<CheckAvailabilityUseCase.AvailabilityDetails> result = checkAvailabilityUseCase.executeDetailed(eventId);
        StepVerifier.create(result)
            .assertNext(details -> {
                assertEquals(1000, details.totalCapacity());
                assertEquals(1000, details.availableTickets());
                assertEquals(0, details.reservedTickets());
                assertEquals(0, details.soldTickets());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle completely sold out scenario")
    void shouldHandleCompletelySoldOutScenario() {
        String eventId = "event-123";
        Event event = new Event(
            eventId, "Sold Out Event", LocalDateTime.now().plusDays(30),
            "Venue", Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0),
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)).thenReturn(Mono.just(0L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.RESERVED)).thenReturn(Mono.just(0L));
        when(ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.SOLD)).thenReturn(Mono.just(1000L));
        Mono<CheckAvailabilityUseCase.AvailabilityDetails> result = checkAvailabilityUseCase.executeDetailed(eventId);
        StepVerifier.create(result)
            .assertNext(details -> {
                assertEquals(1000, details.totalCapacity());
                assertEquals(0, details.availableTickets());
                assertEquals(0, details.reservedTickets());
                assertEquals(1000, details.soldTickets());
            })
            .verifyComplete();
    }
}
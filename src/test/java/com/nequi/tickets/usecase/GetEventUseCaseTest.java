package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
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
@DisplayName("GetEventUseCase Tests")
class GetEventUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    private GetEventUseCase getEventUseCase;
    @BeforeEach
    void setUp() {
        getEventUseCase = new GetEventUseCase(eventRepository);
    }
    @Test
    @DisplayName("Should get event successfully when event exists")
    void shouldGetEventSuccessfully() {
        String eventId = "event-123";
        Event expectedEvent = Event.create(
            eventId,
            "Rock Concert",
            LocalDateTime.now().plusDays(30),
            "Madison Square Garden",
            1000);
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(expectedEvent));
        Mono<Event> result = getEventUseCase.execute(eventId);
        StepVerifier.create(result)
            .assertNext(event -> {
                assertNotNull(event);
                assertEquals(eventId, event.eventId());
                assertEquals("Rock Concert", event.name());
                assertEquals("Madison Square Garden", event.location());
                assertEquals(1000, event.totalCapacity());
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId);
    }
    @Test
    @DisplayName("Should throw EventNotFoundException when event does not exist")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        String eventId = "non-existent-event";
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());
        Mono<Event> result = getEventUseCase.execute(eventId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof EventNotFoundException &&
                throwable.getMessage().contains("Event not found with ID: " + eventId))
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
    }
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw IllegalArgumentException when event ID is null or blank")
    void shouldThrowExceptionWhenEventIdIsNullOrBlank(String invalidEventId) {
        Mono<Event> result = getEventUseCase.execute(invalidEventId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().equals("Event ID is required"))
            .verify();
        verifyNoInteractions(eventRepository);
    }
    @Test
    @DisplayName("Should not call repository when event ID is invalid")
    void shouldNotCallRepositoryWhenEventIdIsInvalid() {
        Mono<Event> result = getEventUseCase.execute("");
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(eventRepository);
    }
    @Test
    @DisplayName("Should handle repository error")
    void shouldHandleRepositoryError() {
        String eventId = "event-123";
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(eventRepository.findById(eventId)).thenReturn(Mono.error(repositoryError));
        Mono<Event> result = getEventUseCase.execute(eventId);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof RuntimeException &&
                throwable.getMessage().equals("Database connection failed"))
            .verify();
        verify(eventRepository, times(1)).findById(eventId);
    }
    @Test
    @DisplayName("Should return complete event with all fields")
    void shouldReturnCompleteEventWithAllFields() {
        String eventId = "event-123";
        LocalDateTime now = LocalDateTime.now();
        Event expectedEvent = new Event(
            eventId,
            "Rock Concert",
            now.plusDays(30),
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(750),
            Integer.valueOf(250),
            Integer.valueOf(0),
            Integer.valueOf(2),
            now.minusDays(5),
            now
        );
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(expectedEvent));
        Mono<Event> result = getEventUseCase.execute(eventId);
        StepVerifier.create(result)
            .assertNext(event -> {
                assertEquals(eventId, event.eventId());
                assertEquals("Rock Concert", event.name());
                assertEquals("Madison Square Garden", event.location());
                assertEquals(1000, event.totalCapacity());
                assertEquals(750, event.availableTickets());
                assertEquals(250, event.reservedTickets());
                assertEquals(2, event.version());
                assertNotNull(event.createdAt());
                assertNotNull(event.updatedAt());
            })
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle multiple sequential calls independently")
    void shouldHandleMultipleSequentialCallsIndependently() {
        String eventId1 = "event-123";
        String eventId2 = "event-456";
        Event event1 = Event.create(eventId1, "Event 1", LocalDateTime.now().plusDays(10), "Location 1", 100);
        Event event2 = Event.create(eventId2, "Event 2", LocalDateTime.now().plusDays(20), "Location 2", 200);
        when(eventRepository.findById(eventId1)).thenReturn(Mono.just(event1));
        when(eventRepository.findById(eventId2)).thenReturn(Mono.just(event2));
        StepVerifier.create(getEventUseCase.execute(eventId1))
            .assertNext(event -> assertEquals(eventId1, event.eventId()))
            .verifyComplete();
        StepVerifier.create(getEventUseCase.execute(eventId2))
            .assertNext(event -> assertEquals(eventId2, event.eventId()))
            .verifyComplete();
        verify(eventRepository, times(1)).findById(eventId1);
        verify(eventRepository, times(1)).findById(eventId2);
    }
}
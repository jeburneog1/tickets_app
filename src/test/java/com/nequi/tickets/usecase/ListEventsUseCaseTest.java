package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListEventsUseCase Tests")
class ListEventsUseCaseTest {
    @Mock
    private EventRepository eventRepository;
    private ListEventsUseCase listEventsUseCase;
    @BeforeEach
    void setUp() {
        listEventsUseCase = new ListEventsUseCase(eventRepository);
    }
    @Test
    @DisplayName("Should list all events successfully")
    void shouldListAllEventsSuccessfully() {
        Event event1 = Event.create(
            "event-1",
            "Rock Concert",
            LocalDateTime.now().plusDays(10),
            "Madison Square Garden",
            1000
        );
        Event event2 = Event.create(
            "event-2",
            "Jazz Festival",
            LocalDateTime.now().plusDays(20),
            "Central Park",
            2000
        );
        Event event3 = Event.create(
            "event-3",
            "Theater Play",
            LocalDateTime.now().plusDays(30),
            "Broadway Theater",
            500
        );
        when(eventRepository.findAll()).thenReturn(Flux.just(event1, event2, event3));
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .assertNext(event -> {
                assertEquals("event-1", event.eventId());
                assertEquals("Rock Concert", event.name());
            })
            .assertNext(event -> {
                assertEquals("event-2", event.eventId());
                assertEquals("Jazz Festival", event.name());
            })
            .assertNext(event -> {
                assertEquals("event-3", event.eventId());
                assertEquals("Theater Play", event.name());
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findAll();
    }
    @Test
    @DisplayName("Should return empty flux when no events exist")
    void shouldReturnEmptyFluxWhenNoEventsExist() {
        when(eventRepository.findAll()).thenReturn(Flux.empty());
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete();
        verify(eventRepository, times(1)).findAll();
    }
    @Test
    @DisplayName("Should handle repository error")
    void shouldHandleRepositoryError() {
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(eventRepository.findAll()).thenReturn(Flux.error(repositoryError));
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof RuntimeException &&
                throwable.getMessage().equals("Database connection failed"))
            .verify();
        verify(eventRepository, times(1)).findAll();
    }
    @Test
    @DisplayName("Should list single event")
    void shouldListSingleEvent() {
        Event event = Event.create(
            "event-1",
            "Solo Concert",
            LocalDateTime.now().plusDays(15),
            "Small Venue",
            100
        );
        when(eventRepository.findAll()).thenReturn(Flux.just(event));
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();
        verify(eventRepository, times(1)).findAll();
    }
    @Test
    @DisplayName("Should list events with available tickets only")
    void shouldListEventsWithAvailableTicketsOnly() {
        Event event1 = new Event(
            "event-1", "Available Event", LocalDateTime.now().plusDays(10),
            "Location 1", Integer.valueOf(1000), Integer.valueOf(500), Integer.valueOf(500), Integer.valueOf(0), Integer.valueOf(0),
            LocalDateTime.now(), LocalDateTime.now()
        );
        Event event2 = new Event(
            "event-2", "Another Available Event", LocalDateTime.now().plusDays(20),
            "Location 2", Integer.valueOf(2000), Integer.valueOf(1000), Integer.valueOf(1000), Integer.valueOf(0), Integer.valueOf(0),
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(eventRepository.findEventsWithAvailability()).thenReturn(Flux.just(event1, event2));
        Flux<Event> result = listEventsUseCase.executeWithAvailability();
        StepVerifier.create(result)
            .assertNext(event -> {
                assertEquals("event-1", event.eventId());
                assertTrue(event.availableTickets() > 0);
            })
            .assertNext(event -> {
                assertEquals("event-2", event.eventId());
                assertTrue(event.availableTickets() > 0);
            })
            .verifyComplete();
        verify(eventRepository, times(1)).findEventsWithAvailability();
    }
    @Test
    @DisplayName("Should return empty flux when no events with availability")
    void shouldReturnEmptyFluxWhenNoEventsWithAvailability() {
        when(eventRepository.findEventsWithAvailability()).thenReturn(Flux.empty());
        Flux<Event> result = listEventsUseCase.executeWithAvailability();
        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete();
        verify(eventRepository, times(1)).findEventsWithAvailability();
    }
    @Test
    @DisplayName("Should list upcoming events only")
    void shouldListUpcomingEventsOnly() {
        LocalDateTime now = LocalDateTime.now();
        Event event1 = Event.create(
            "event-1", "Upcoming Event 1", now.plusDays(5), "Location 1", 100
        );
        Event event2 = Event.create(
            "event-2", "Upcoming Event 2", now.plusDays(15), "Location 2", 200
        );
        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class)))
            .thenReturn(Flux.just(event1, event2));
        Flux<Event> result = listEventsUseCase.executeUpcoming();
        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete();
        verify(eventRepository, times(1)).findUpcomingEvents(any(LocalDateTime.class));
    }
    @Test
    @DisplayName("Should return empty flux when no upcoming events")
    void shouldReturnEmptyFluxWhenNoUpcomingEvents() {
        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class)))
            .thenReturn(Flux.empty());
        Flux<Event> result = listEventsUseCase.executeUpcoming();
        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete();
        verify(eventRepository, times(1)).findUpcomingEvents(any(LocalDateTime.class));
    }
    @Test
    @DisplayName("Should handle large number of events")
    void shouldHandleLargeNumberOfEvents() {
        List<Event> events = generateManyEvents(100);
        when(eventRepository.findAll()).thenReturn(Flux.fromIterable(events));
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .expectNextCount(100)
            .verifyComplete();
        verify(eventRepository, times(1)).findAll();
    }
    @Test
    @DisplayName("Should stream events one by one")
    void shouldStreamEventsOneByOne() {
        Event event1 = Event.create("event-1", "Event 1", LocalDateTime.now().plusDays(10), "Location 1", 100);
        Event event2 = Event.create("event-2", "Event 2", LocalDateTime.now().plusDays(20), "Location 2", 200);
        when(eventRepository.findAll()).thenReturn(Flux.just(event1, event2));
        Flux<Event> result = listEventsUseCase.execute();
        StepVerifier.create(result)
            .expectNext(event1)
            .expectNext(event2)
            .verifyComplete();
    }
    @Test
    @DisplayName("Should handle repository error when listing with availability")
    void shouldHandleRepositoryErrorWhenListingWithAvailability() {
        RuntimeException repositoryError = new RuntimeException("Query failed");
        when(eventRepository.findEventsWithAvailability()).thenReturn(Flux.error(repositoryError));
        Flux<Event> result = listEventsUseCase.executeWithAvailability();
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
        verify(eventRepository, times(1)).findEventsWithAvailability();
    }
    private List<Event> generateManyEvents(int count) {
        List<Event> events = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            events.add(Event.create(
                "event-" + i,
                "Event " + i,
                LocalDateTime.now().plusDays(i),
                "Location " + i,
                100 * i));
        }
        return events;
    }
}
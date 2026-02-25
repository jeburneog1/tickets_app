package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.infrastructure.dto.CreateEventRequest;
import com.nequi.tickets.infrastructure.dto.EventResponse;
import com.nequi.tickets.usecase.CreateEventUseCase;
import com.nequi.tickets.usecase.GetEventUseCase;
import com.nequi.tickets.usecase.ListEventsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventController Tests")
class EventControllerTest {
    @Mock
    private CreateEventUseCase createEventUseCase;
    @Mock
    private GetEventUseCase getEventUseCase;
    @Mock
    private ListEventsUseCase listEventsUseCase;
    private WebTestClient webTestClient;
    @BeforeEach
    void setUp() {
        EventController eventController = new EventController(
            createEventUseCase,
            getEventUseCase,
            listEventsUseCase
        );
        webTestClient = WebTestClient.bindToController(eventController)
            .controllerAdvice(new GlobalExceptionHandler())
            .build();
    }
    @Test
    @DisplayName("POST /events - Should create event successfully")
    void shouldCreateEventSuccessfully() {
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        CreateEventRequest request = new CreateEventRequest(
            "Rock Concert",
            eventDate,
            "Madison Square Garden",
            1000);
        Event createdEvent = Event.create("event-123", "Rock Concert", eventDate, "Madison Square Garden", 1000);
        when(createEventUseCase.execute(anyString(), any(), anyString(), anyInt()))
            .thenReturn(Mono.just(createdEvent));
        webTestClient.post()
            .uri("/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.eventId").isEqualTo("event-123")
            .jsonPath("$.name").isEqualTo("Rock Concert")
            .jsonPath("$.location").isEqualTo("Madison Square Garden")
            .jsonPath("$.totalCapacity").isEqualTo(1000)
            .jsonPath("$.availableTickets").isEqualTo(1000)
            .jsonPath("$.reservedTickets").isEqualTo(0);
        verify(createEventUseCase).execute("Rock Concert", eventDate, "Madison Square Garden", 1000);
    }
    @Test
    @DisplayName("GET /events/{eventId} - Should return event with availability details")
    void shouldGetEventSuccessfully() {
        String eventId = "event-123";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        Event event = Event.create(eventId, "Rock Concert", eventDate, "Madison Square Garden", 1000);
        when(getEventUseCase.execute(eventId)).thenReturn(Mono.just(event));
        webTestClient.get()
            .uri("/events/{eventId}", eventId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.eventId").isEqualTo(eventId)
            .jsonPath("$.eventName").isEqualTo("Rock Concert")
            .jsonPath("$.date").exists()
            .jsonPath("$.location").isEqualTo("Madison Square Garden")
            .jsonPath("$.totalCapacity").isEqualTo(1000)
            .jsonPath("$.availableTickets").isEqualTo(1000)
            .jsonPath("$.reservedTickets").isEqualTo(0)
            .jsonPath("$.soldTickets").isEqualTo(0)
            .jsonPath("$.isAvailable").isEqualTo(true)
            .jsonPath("$.createdAt").exists()
            .jsonPath("$.updatedAt").exists();
        verify(getEventUseCase).execute(eventId);
    }
    @Test
    @DisplayName("GET /events - Should list all events successfully")
    void shouldListAllEventsSuccessfully() {
        LocalDateTime eventDate1 = LocalDateTime.now().plusDays(30);
        LocalDateTime eventDate2 = LocalDateTime.now().plusDays(60);
        Event event1 = Event.create("event-1", "Rock Concert", eventDate1, "Madison Square Garden", 1000);
        Event event2 = Event.create("event-2", "Jazz Night", eventDate2, "Blue Note", 500);
        when(listEventsUseCase.execute()).thenReturn(Flux.just(event1, event2));
        webTestClient.get()
            .uri("/events")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(EventResponse.class)
            .hasSize(2)
            .value(events -> {
                assert events.get(0).eventId().equals("event-1");
                assert events.get(0).name().equals("Rock Concert");
                assert events.get(1).eventId().equals("event-2");
                assert events.get(1).name().equals("Jazz Night");
            });
        verify(listEventsUseCase).execute();
    }
    @Test
    @DisplayName("GET /events - Should return empty list when no events exist")
    void shouldReturnEmptyListWhenNoEvents() {
        when(listEventsUseCase.execute()).thenReturn(Flux.empty());
        webTestClient.get()
            .uri("/events")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(EventResponse.class)
            .hasSize(0);
        verify(listEventsUseCase).execute();
    }
    @Test
    @DisplayName("GET /events/{eventId} - Should show unavailable when no tickets left")
    void shouldShowUnavailableWhenNoTicketsLeft() {
        String eventId = "event-123";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event(
            eventId,
            "Rock Concert",
            eventDate,
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(0),  
            Integer.valueOf(0),
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        when(getEventUseCase.execute(eventId)).thenReturn(Mono.just(event));
        webTestClient.get()
            .uri("/events/{eventId}", eventId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.eventId").isEqualTo(eventId)
            .jsonPath("$.availableTickets").isEqualTo(0)
            .jsonPath("$.soldTickets").isEqualTo(1000)
            .jsonPath("$.isAvailable").isEqualTo(false)
            .jsonPath("$.date").exists()
            .jsonPath("$.location").exists()
            .jsonPath("$.createdAt").exists()
            .jsonPath("$.updatedAt").exists();
        verify(getEventUseCase).execute(eventId);
    }
    @Test
    @DisplayName("GET /events/{eventId} - Should calculate sold tickets correctly")
    void shouldCalculateSoldTicketsCorrectly() {
        String eventId = "event-123";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event(
            eventId,
            "Rock Concert",
            eventDate,
            "Madison Square Garden",
            Integer.valueOf(1000),
            Integer.valueOf(300),  
            Integer.valueOf(200),
            Integer.valueOf(0),
            Integer.valueOf(0),
            now,
            now
        );
        when(getEventUseCase.execute(eventId)).thenReturn(Mono.just(event));
        webTestClient.get()
            .uri("/events/{eventId}", eventId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.eventId").isEqualTo(eventId)
            .jsonPath("$.availableTickets").isEqualTo(300)
            .jsonPath("$.reservedTickets").isEqualTo(200)
            .jsonPath("$.soldTickets").isEqualTo(500)
            .jsonPath("$.isAvailable").isEqualTo(true)
            .jsonPath("$.date").exists()
            .jsonPath("$.location").exists()
            .jsonPath("$.createdAt").exists()
            .jsonPath("$.updatedAt").exists();
        verify(getEventUseCase).execute(eventId);
    }
}
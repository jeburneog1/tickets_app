package com.nequi.tickets.integration.repository;

import com.nequi.tickets.domain.exception.ConcurrentModificationException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.EventEntity;
import com.nequi.tickets.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
class EventRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbClient;
    private DynamoDbAsyncTable<EventEntity> eventTable;
    @BeforeEach
    void setUp() {
        eventTable = dynamoDbClient.table("events", 
            TableSchema.fromBean(EventEntity.class));
        Flux.from(eventTable.scan().items())
            .flatMap(entity -> Mono.fromCompletionStage(() -> 
                eventTable.deleteItem(entity)))
            .blockLast();
    }
    @Test
    void shouldSaveAndFindEventById() {
        Event event = createTestEvent("event-1", "Concert", 100);
        StepVerifier.create(eventRepository.save(event))
            .assertNext(savedEvent -> {
                assertThat(savedEvent.eventId()).isEqualTo("event-1");
                assertThat(savedEvent.name()).isEqualTo("Concert");
                assertThat(savedEvent.totalCapacity()).isEqualTo(100);
            })
            .verifyComplete();
        StepVerifier.create(eventRepository.findById("event-1"))
            .assertNext(foundEvent -> {
                assertThat(foundEvent.eventId()).isEqualTo("event-1");
                assertThat(foundEvent.name()).isEqualTo("Concert");
                assertThat(foundEvent.availableTickets()).isEqualTo(100);
            })
            .verifyComplete();
    }
    @Test
    void shouldReturnEmptyWhenEventNotFound() {
        StepVerifier.create(eventRepository.findById("non-existent"))
            .verifyComplete();
    }
    @Test
    void shouldFindAllEvents() {
        Event event1 = createTestEvent("event-1", "Concert", 100);
        Event event2 = createTestEvent("event-2", "Theater", 50);
        Event event3 = createTestEvent("event-3", "Sports", 200);
        Flux.merge(
            eventRepository.save(event1),
            eventRepository.save(event2),
            eventRepository.save(event3)
        ).blockLast();
        StepVerifier.create(eventRepository.findAll())
            .expectNextCount(3)
            .verifyComplete();
    }
    @Test
    void shouldFindUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        Event pastEvent = createTestEventWithDate("event-1", "Past", 100, now.minusDays(1));
        Event futureEvent1 = createTestEventWithDate("event-2", "Future1", 100, now.plusDays(1));
        Event futureEvent2 = createTestEventWithDate("event-3", "Future2", 100, now.plusDays(2));
        Flux.merge(
            eventRepository.save(pastEvent),
            eventRepository.save(futureEvent1),
            eventRepository.save(futureEvent2)
        ).blockLast();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(eventRepository.findUpcomingEvents(now))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindEventsWithAvailability() {
        Event noAvailability = createTestEventWithAvailability("event-1", "NoAvail", 0, 10);
        Event withAvailability1 = createTestEventWithAvailability("event-2", "Avail1", 50, 0);
        Event withAvailability2 = createTestEventWithAvailability("event-3", "Avail2", 100, 0);
        Flux.merge(
            eventRepository.save(noAvailability),
            eventRepository.save(withAvailability1),
            eventRepository.save(withAvailability2)
        ).blockLast();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(eventRepository.findEventsWithAvailability())
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldCheckExistence() {
        Event event = createTestEvent("event-1", "Concert", 100);
        eventRepository.save(event).block();
        StepVerifier.create(eventRepository.existsById("event-1"))
            .expectNext(true)
            .verifyComplete();
        StepVerifier.create(eventRepository.existsById("non-existent"))
            .expectNext(false)
            .verifyComplete();
    }
    @Test
    void shouldDeleteEvent() {
        Event event = createTestEvent("event-1", "Concert", 100);
        eventRepository.save(event).block();
        StepVerifier.create(eventRepository.deleteById("event-1"))
            .verifyComplete();
        StepVerifier.create(eventRepository.findById("event-1"))
            .verifyComplete();
    }
    @Test
    void shouldUpdateInventorySuccessfully() {
        Event event = createTestEventWithAvailability("event-1", "Concert", 100, 0);
        Event savedEvent = eventRepository.save(event).block();
        StepVerifier.create(eventRepository.updateInventory(
            "event-1", 
            savedEvent.version(), 
            80, 
            20))
            .assertNext(updated -> {
                assertThat(updated.availableTickets()).isEqualTo(80);
                assertThat(updated.reservedTickets()).isEqualTo(20);
                assertThat(updated.version()).isEqualTo(savedEvent.version() + 1);
            })
            .verifyComplete();
        StepVerifier.create(eventRepository.findById("event-1"))
            .assertNext(found -> {
                assertThat(found.availableTickets()).isEqualTo(80);
                assertThat(found.reservedTickets()).isEqualTo(20);
            })
            .verifyComplete();
    }
    @Test
    void shouldFailUpdateInventoryWithWrongVersion() {
        Event event = createTestEvent("event-1", "Concert", 100);
        Event savedEvent = eventRepository.save(event).block();
        StepVerifier.create(eventRepository.updateInventory(
            "event-1", 
            999, 
            80, 
            20))
            .expectError(ConcurrentModificationException.class)
            .verify();
    }
    @Test
    void shouldFailUpdateInventoryForNonExistentEvent() {
        StepVerifier.create(eventRepository.updateInventory(
            "non-existent", 
            0, 
            80, 
            20))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldHandleConcurrentModifications() {
        Event event = createTestEvent("event-1", "Concert", 100);
        Event savedEvent = eventRepository.save(event).block();
        Mono<Event> update1 = eventRepository.updateInventory(
            savedEvent.eventId(), 
            savedEvent.version(), 
            90, 
            10);
        Mono<Event> update2 = eventRepository.updateInventory(
            savedEvent.eventId(), 
            savedEvent.version(), 
            80, 
            20);
        Event result1 = update1.block();
        assertThat(result1).isNotNull();
        StepVerifier.create(update2)
            .expectError(ConcurrentModificationException.class)
            .verify();
    }
    @Test
    void shouldUpdateEventMultipleTimes() {
        Event event = createTestEvent("event-1", "Concert", 100);
        Event saved = eventRepository.save(event).block();
        Event updated1 = eventRepository.updateInventory(
            saved.eventId(), saved.version(), 90, 10).block();
        Event updated2 = eventRepository.updateInventory(
            updated1.eventId(), updated1.version(), 80, 20).block();
        Event updated3 = eventRepository.updateInventory(
            updated2.eventId(), updated2.version(), 70, 30).block();
        assertThat(updated3.availableTickets()).isEqualTo(70);
        assertThat(updated3.reservedTickets()).isEqualTo(30);
        assertThat(updated3.version()).isEqualTo(saved.version() + 3);
    }
    private Event createTestEvent(String id, String name, int capacity) {
        return new Event(
            id,
            name,
            LocalDateTime.now().plusDays(7),
            "Test Location",
            Integer.valueOf(capacity),
            Integer.valueOf(capacity),
            Integer.valueOf(0),
            Integer.valueOf(0),
            Integer.valueOf(0),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    private Event createTestEventWithDate(String id, String name, int capacity, LocalDateTime date) {
        return new Event(
            id,
            name,
            date,
            "Test Location",
            Integer.valueOf(capacity),
            Integer.valueOf(capacity),
            Integer.valueOf(0),
            Integer.valueOf(0),
            Integer.valueOf(0),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    private Event createTestEventWithAvailability(String id, String name, 
                                                   int available, int reserved) {
        int total = available + reserved;
        return new Event(
            id,
            name,
            LocalDateTime.now().plusDays(7),
            "Test Location",
            Integer.valueOf(total),
            Integer.valueOf(available),
            Integer.valueOf(reserved),
            Integer.valueOf(0),
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
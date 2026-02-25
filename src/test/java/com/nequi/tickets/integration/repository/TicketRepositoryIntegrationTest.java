package com.nequi.tickets.integration.repository;

import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.TicketEntity;
import com.nequi.tickets.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
class TicketRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbClient;
    private DynamoDbAsyncTable<TicketEntity> ticketTable;
    @BeforeEach
    void setUp() {
        ticketTable = dynamoDbClient.table("tickets", 
            TableSchema.fromBean(TicketEntity.class));
        Flux.from(ticketTable.scan().items())
            .flatMap(entity -> Mono.fromCompletionStage(() -> 
                ticketTable.deleteItem(entity)))
            .blockLast();
    }
    @Test
    void shouldSaveAndFindTicketById() {
        Ticket ticket = createAvailableTicket("ticket-1", "event-1");
        StepVerifier.create(ticketRepository.save(ticket))
            .assertNext(saved -> {
                assertThat(saved.ticketId()).isEqualTo("ticket-1");
                assertThat(saved.eventId()).isEqualTo("event-1");
                assertThat(saved.status()).isEqualTo(TicketStatus.AVAILABLE);
            })
            .verifyComplete();
        StepVerifier.create(ticketRepository.findById("ticket-1"))
            .assertNext(found -> {
                assertThat(found.ticketId()).isEqualTo("ticket-1");
                assertThat(found.status()).isEqualTo(TicketStatus.AVAILABLE);
            })
            .verifyComplete();
    }
    @Test
    void shouldReturnEmptyWhenTicketNotFound() {
        StepVerifier.create(ticketRepository.findById("non-existent"))
            .verifyComplete();
    }
    @Test
    void shouldSaveAllTickets() {
        List<Ticket> tickets = Arrays.asList(
            createAvailableTicket("ticket-1", "event-1"),
            createAvailableTicket("ticket-2", "event-1"),
            createAvailableTicket("ticket-3", "event-1")
        );
        StepVerifier.create(ticketRepository.saveAll(tickets))
            .expectNextCount(3)
            .verifyComplete();
        StepVerifier.create(ticketRepository.findById("ticket-1"))
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldFindMultipleTicketsByIds() {
        Ticket ticket1 = createAvailableTicket("ticket-1", "event-1");
        Ticket ticket2 = createAvailableTicket("ticket-2", "event-1");
        Ticket ticket3 = createAvailableTicket("ticket-3", "event-1");
        Flux.merge(
            ticketRepository.save(ticket1),
            ticketRepository.save(ticket2),
            ticketRepository.save(ticket3)
        ).blockLast();
        StepVerifier.create(ticketRepository.findByIds(
            Arrays.asList("ticket-1", "ticket-2", "ticket-3")))
            .expectNextCount(3)
            .verifyComplete();
    }
    @Test
    void shouldFindTicketsByEventId() {
        Ticket ticket1 = createAvailableTicket("ticket-1", "event-1");
        Ticket ticket2 = createAvailableTicket("ticket-2", "event-1");
        Ticket ticket3 = createAvailableTicket("ticket-3", "event-2");
        Flux.merge(
            ticketRepository.save(ticket1),
            ticketRepository.save(ticket2),
            ticketRepository.save(ticket3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.findByEventId("event-1"))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindTicketsByEventIdAndStatus() {
        Ticket available = createAvailableTicket("ticket-1", "event-1");
        Ticket reserved = createReservedTicket("ticket-2", "event-1", "customer-1");
        Ticket available2 = createAvailableTicket("ticket-3", "event-1");
        Flux.merge(
            ticketRepository.save(available),
            ticketRepository.save(reserved),
            ticketRepository.save(available2)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.findByEventIdAndStatus(
            "event-1", TicketStatus.AVAILABLE))
            .expectNextCount(2)
            .verifyComplete();
        StepVerifier.create(ticketRepository.findByEventIdAndStatus(
            "event-1", TicketStatus.RESERVED))
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldFindTicketsByCustomerId() {
        Ticket reserved1 = createReservedTicket("ticket-1", "event-1", "customer-1");
        Ticket reserved2 = createReservedTicket("ticket-2", "event-1", "customer-1");
        Ticket reserved3 = createReservedTicket("ticket-3", "event-1", "customer-2");
        Flux.merge(
            ticketRepository.save(reserved1),
            ticketRepository.save(reserved2),
            ticketRepository.save(reserved3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.findByCustomerId("customer-1"))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindTicketsByOrderId() {
        Ticket ticket1 = createTicketWithOrder("ticket-1", "event-1", "customer-1", "order-1");
        Ticket ticket2 = createTicketWithOrder("ticket-2", "event-1", "customer-1", "order-1");
        Ticket ticket3 = createTicketWithOrder("ticket-3", "event-1", "customer-2", "order-2");
        Flux.merge(
            ticketRepository.save(ticket1),
            ticketRepository.save(ticket2),
            ticketRepository.save(ticket3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.findByOrderId("order-1"))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        Ticket expired1 = createReservedTicketWithExpiration("ticket-1", "event-1", 
            "customer-1", now.minusMinutes(5));
        Ticket expired2 = createReservedTicketWithExpiration("ticket-2", "event-1", 
            "customer-2", now.minusMinutes(1));
        Ticket valid = createReservedTicketWithExpiration("ticket-3", "event-1", 
            "customer-3", now.plusMinutes(5));
        
        // Save tickets sequentially and wait for persistence
        ticketRepository.save(expired1).block();
        ticketRepository.save(expired2).block();
        ticketRepository.save(valid).block();
        
        // Give DynamoDB time to persist
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> ticketRepository.findById("ticket-1").hasElement().block());
        
        StepVerifier.create(ticketRepository.findExpiredReservations(now))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldCountAvailableTicketsByEventId() {
        Ticket available1 = createAvailableTicket("ticket-1", "event-1");
        Ticket available2 = createAvailableTicket("ticket-2", "event-1");
        Ticket reserved = createReservedTicket("ticket-3", "event-1", "customer-1");
        Flux.merge(
            ticketRepository.save(available1),
            ticketRepository.save(available2),
            ticketRepository.save(reserved)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.countAvailableByEventId("event-1"))
            .expectNext(2L)
            .verifyComplete();
    }
    @Test
    void shouldCountTicketsByEventIdAndStatus() {
        Ticket available = createAvailableTicket("ticket-1", "event-1");
        Ticket reserved1 = createReservedTicket("ticket-2", "event-1", "customer-1");
        Ticket reserved2 = createReservedTicket("ticket-3", "event-1", "customer-2");
        Flux.merge(
            ticketRepository.save(available),
            ticketRepository.save(reserved1),
            ticketRepository.save(reserved2)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.countByEventIdAndStatus(
            "event-1", TicketStatus.RESERVED))
            .expectNext(2L)
            .verifyComplete();
        StepVerifier.create(ticketRepository.countByEventIdAndStatus(
            "event-1", TicketStatus.AVAILABLE))
            .expectNext(1L)
            .verifyComplete();
    }
    @Test
    void shouldDeleteTicket() {
        Ticket ticket = createAvailableTicket("ticket-1", "event-1");
        ticketRepository.save(ticket).block();
        StepVerifier.create(ticketRepository.deleteById("ticket-1"))
            .verifyComplete();
        StepVerifier.create(ticketRepository.findById("ticket-1"))
            .verifyComplete();
    }
    @Test
    void shouldDeleteAllTicketsByEventId() {
        Ticket ticket1 = createAvailableTicket("ticket-1", "event-1");
        Ticket ticket2 = createAvailableTicket("ticket-2", "event-1");
        Ticket ticket3 = createAvailableTicket("ticket-3", "event-2");
        Flux.merge(
            ticketRepository.save(ticket1),
            ticketRepository.save(ticket2),
            ticketRepository.save(ticket3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(ticketRepository.deleteByEventId("event-1"))
            .verifyComplete();
        StepVerifier.create(ticketRepository.findByEventId("event-1"))
            .verifyComplete();
        StepVerifier.create(ticketRepository.findById("ticket-3"))
            .expectNextCount(1)
            .verifyComplete();
    }
    private Ticket createAvailableTicket(String ticketId, String eventId) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.AVAILABLE,
            null,
            null,
            null,
            null,
            0,
            now,
            now
        );
    }
    private Ticket createReservedTicket(String ticketId, String eventId, String customerId) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.RESERVED,
            customerId,
            null,
            now,
            now.plusMinutes(10),
            0,
            now,
            now
        );
    }
    private Ticket createReservedTicketWithExpiration(String ticketId, String eventId, 
                                                      String customerId, 
                                                      LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.PENDING_CONFIRMATION,
            customerId,
            null,
            now,
            expiresAt,
            0,
            now,
            now
        );
    }
    private Ticket createTicketWithOrder(String ticketId, String eventId, 
                                         String customerId, String orderId) {
        LocalDateTime now = LocalDateTime.now();
        return new Ticket(
            ticketId,
            eventId,
            TicketStatus.PENDING_CONFIRMATION,
            customerId,
            orderId,
            now,
            now.plusMinutes(10),
            0,
            now,
            now
        );
    }
}
package com.nequi.tickets.integration.repository;

import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.OrderEntity;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
class OrderRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbClient;
    private DynamoDbAsyncTable<OrderEntity> orderTable;
    @BeforeEach
    void setUp() {
        orderTable = dynamoDbClient.table("orders", 
            TableSchema.fromBean(OrderEntity.class));
        Flux.from(orderTable.scan().items())
            .flatMap(entity -> Mono.fromCompletionStage(() -> 
                orderTable.deleteItem(entity)))
            .blockLast();
    }
    @Test
    void shouldSaveAndFindOrderById() {
        Order order = createTestOrder("order-1", "event-1", "customer-1", 
            Arrays.asList("ticket-1", "ticket-2"), OrderStatus.PENDING);
        StepVerifier.create(orderRepository.save(order))
            .assertNext(saved -> {
                assertThat(saved.orderId()).isEqualTo("order-1");
                assertThat(saved.eventId()).isEqualTo("event-1");
                assertThat(saved.customerId()).isEqualTo("customer-1");
                assertThat(saved.totalTickets()).isEqualTo(2);
                assertThat(saved.status()).isEqualTo(OrderStatus.PENDING);
            })
            .verifyComplete();
        StepVerifier.create(orderRepository.findById("order-1"))
            .assertNext(found -> {
                assertThat(found.orderId()).isEqualTo("order-1");
                assertThat(found.ticketIds()).hasSize(2);
            })
            .verifyComplete();
    }
    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        StepVerifier.create(orderRepository.findById("non-existent"))
            .verifyComplete();
    }
    @Test
    void shouldFindMultipleOrdersByIds() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-1", "customer-3", 
            List.of("ticket-3"), OrderStatus.FAILED);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        StepVerifier.create(orderRepository.findByIds(
            Arrays.asList("order-1", "order-2", "order-3")))
            .expectNextCount(3)
            .verifyComplete();
    }
    @Test
    void shouldFindOrdersByCustomerId() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-2", "customer-1", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-1", "customer-2", 
            List.of("ticket-3"), OrderStatus.PENDING);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findByCustomerId("customer-1"))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindOrdersByEventId() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-2", "customer-3", 
            List.of("ticket-3"), OrderStatus.PENDING);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findByEventId("event-1"))
            .expectNextCount(2)
            .verifyComplete();
    }
    @Test
    void shouldFindOrdersByStatus() {
        Order pending1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order pending2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.PENDING);
        Order confirmed = createTestOrder("order-3", "event-1", "customer-3", 
            List.of("ticket-3"), OrderStatus.CONFIRMED);
        Flux.merge(
            orderRepository.save(pending1),
            orderRepository.save(pending2),
            orderRepository.save(confirmed)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findByStatus(OrderStatus.PENDING))
            .expectNextCount(2)
            .verifyComplete();
        StepVerifier.create(orderRepository.findByStatus(OrderStatus.CONFIRMED))
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldFindPendingOrders() {
        Order pending = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order confirmed = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Flux.merge(
            orderRepository.save(pending),
            orderRepository.save(confirmed)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findPendingOrders())
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldFindOrdersByCustomerIdAndStatus() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-1", "customer-1", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-1", "customer-2", 
            List.of("ticket-3"), OrderStatus.PENDING);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findByCustomerIdAndStatus(
            "customer-1", OrderStatus.PENDING))
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldFindOrdersByEventIdAndStatus() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-2", "customer-3", 
            List.of("ticket-3"), OrderStatus.PENDING);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.findByEventIdAndStatus(
            "event-1", OrderStatus.PENDING))
            .expectNextCount(1)
            .verifyComplete();
    }
    @Test
    void shouldCheckOrderExistence() {
        Order order = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        orderRepository.save(order).block();
        StepVerifier.create(orderRepository.existsById("order-1"))
            .expectNext(true)
            .verifyComplete();
        StepVerifier.create(orderRepository.existsById("non-existent"))
            .expectNext(false)
            .verifyComplete();
    }
    @Test
    void shouldCountOrdersByStatus() {
        Order pending1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order pending2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.PENDING);
        Order confirmed = createTestOrder("order-3", "event-1", "customer-3", 
            List.of("ticket-3"), OrderStatus.CONFIRMED);
        Flux.merge(
            orderRepository.save(pending1),
            orderRepository.save(pending2),
            orderRepository.save(confirmed)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.countByStatus(OrderStatus.PENDING))
            .expectNext(2L)
            .verifyComplete();
    }
    @Test
    void shouldCountOrdersByEventId() {
        Order order1 = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        Order order2 = createTestOrder("order-2", "event-1", "customer-2", 
            List.of("ticket-2"), OrderStatus.CONFIRMED);
        Order order3 = createTestOrder("order-3", "event-2", "customer-3", 
            List.of("ticket-3"), OrderStatus.PENDING);
        Flux.merge(
            orderRepository.save(order1),
            orderRepository.save(order2),
            orderRepository.save(order3)
        ).blockLast();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StepVerifier.create(orderRepository.countByEventId("event-1"))
            .expectNext(2L)
            .verifyComplete();
    }
    @Test
    void shouldDeleteOrder() {
        Order order = createTestOrder("order-1", "event-1", "customer-1", 
            List.of("ticket-1"), OrderStatus.PENDING);
        orderRepository.save(order).block();
        StepVerifier.create(orderRepository.deleteById("order-1"))
            .verifyComplete();
        StepVerifier.create(orderRepository.findById("order-1"))
            .verifyComplete();
    }
    private Order createTestOrder(String orderId, String eventId, String customerId, 
                                  List<String> ticketIds, OrderStatus status) {
        return new Order(
            orderId,
            eventId,
            customerId,
            ticketIds,
            status,
            ticketIds.size(),
            0,
            0,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );
    }
}
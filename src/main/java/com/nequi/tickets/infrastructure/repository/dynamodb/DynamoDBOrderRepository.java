package com.nequi.tickets.infrastructure.repository.dynamodb;

import com.nequi.tickets.domain.exception.ConcurrentModificationException;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.OrderEntity;
import com.nequi.tickets.infrastructure.repository.dynamodb.mapper.EntityMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;

@Repository
public class DynamoDBOrderRepository implements OrderRepository {
    
    private final DynamoDbAsyncTable<OrderEntity> orderTable;
    private final DynamoDbAsyncIndex<OrderEntity> customerIndex;
    private final DynamoDbAsyncIndex<OrderEntity> eventIndex;
    private final DynamoDbAsyncIndex<OrderEntity> statusIndex;
    
    public DynamoDBOrderRepository(
            DynamoDbEnhancedAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.tables.orders}") String tableName) {
        this.orderTable = dynamoDbClient.table(tableName, TableSchema.fromBean(OrderEntity.class));
        this.customerIndex = orderTable.index("customerId-index");
        this.eventIndex = orderTable.index("eventId-index");
        this.statusIndex = orderTable.index("status-index");
    }
    
    @Override
    public Mono<Order> save(Order order) {
        OrderEntity entity = EntityMapper.toOrderEntity(order);
        
        return Mono.fromCompletionStage(() -> orderTable.putItem(entity))
            .thenReturn(order)
            .onErrorMap(ConditionalCheckFailedException.class, 
                ex -> new ConcurrentModificationException(
                    "Order", order.orderId()));
    }
    
    @Override
    public Mono<Order> findById(String orderId) {
        Key key = Key.builder()
            .partitionValue(orderId)
            .build();
        
        return Mono.fromCompletionStage(() -> orderTable.getItem(key))
            .map(EntityMapper::toOrder);
    }
    
    @Override
    public Flux<Order> findByIds(List<String> orderIds) {
        return Flux.fromIterable(orderIds)
            .flatMap(this::findById)
            .onErrorContinue((error, item) -> {
            });
    }
    
    @Override
    public Flux<Order> findByCustomerId(String customerId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(customerId).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(customerIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toOrder);
    }
    
    @Override
    public Flux<Order> findByEventId(String eventId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(eventId).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(eventIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toOrder);
    }
    
    @Override
    public Flux<Order> findByStatus(OrderStatus status) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(status.name()).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(statusIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toOrder);
    }
    
    @Override
    public Flux<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status) {
        return findByCustomerId(customerId)
            .filter(order -> order.status().equals(status));
    }
    
    @Override
    public Flux<Order> findByEventIdAndStatus(String eventId, OrderStatus status) {
        return findByEventId(eventId)
            .filter(order -> order.status().equals(status));
    }
    
    @Override
    public Flux<Order> findPendingOrders() {
        return findByStatus(OrderStatus.PENDING);
    }
    
    @Override
    public Mono<Boolean> existsById(String orderId) {
        return findById(orderId)
            .map(order -> true)
            .defaultIfEmpty(false);
    }
    
    @Override
    public Mono<Long> countByStatus(OrderStatus status) {
        return findByStatus(status).count();
    }
    
    @Override
    public Mono<Long> countByEventId(String eventId) {
        return findByEventId(eventId).count();
    }
    
    @Override
    public Mono<Void> deleteById(String orderId) {
        Key key = Key.builder()
            .partitionValue(orderId)
            .build();
        
        return Mono.fromCompletionStage(() -> orderTable.deleteItem(key))
            .then();
    }
}

package com.nequi.tickets.infrastructure.repository.dynamodb;

import com.nequi.tickets.domain.exception.ConcurrentModificationException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.EventEntity;
import com.nequi.tickets.infrastructure.repository.dynamodb.mapper.EntityMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class DynamoDBEventRepository implements EventRepository {
    
    private final DynamoDbAsyncTable<EventEntity> eventTable;
    
    public DynamoDBEventRepository(
            DynamoDbEnhancedAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.tables.events}") String tableName) {
        this.eventTable = dynamoDbClient.table(tableName, TableSchema.fromBean(EventEntity.class));
    }
    
    @Override
    public Mono<Event> save(Event event) {
        EventEntity entity = EntityMapper.toEventEntity(event);
        
        return Mono.fromCompletionStage(() -> eventTable.putItem(entity))
            .thenReturn(event)
            .onErrorMap(ConditionalCheckFailedException.class, 
                ex -> new ConcurrentModificationException(
                    "Event", event.eventId()));
    }
    
    @Override
    public Mono<Event> findById(String eventId) {
        Key key = Key.builder()
            .partitionValue(eventId)
            .build();
        
        return Mono.fromCompletionStage(() -> eventTable.getItem(key))
            .map(EntityMapper::toEvent);
    }
    
    @Override
    public Flux<Event> findAll() {
        return Flux.from(eventTable.scan().items())
            .map(EntityMapper::toEvent);
    }
    
    @Override
    public Flux<Event> findUpcomingEvents(LocalDateTime startDate) {
        String startDateStr = startDate.toString();
        
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#date", "date");
        
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":startDate", AttributeValue.builder().s(startDateStr).build());
        
        Expression filterExpression = Expression.builder()
            .expression("#date >= :startDate")
            .expressionNames(expressionNames)
            .expressionValues(expressionValues)
            .build();
        
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(filterExpression)
            .build();
        
        return Flux.from(eventTable.scan(scanRequest).items())
            .map(EntityMapper::toEvent);
    }
    
    @Override
    public Flux<Event> findEventsWithAvailability() {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());
        
        Expression filterExpression = Expression.builder()
            .expression("availableTickets > :zero")
            .expressionValues(expressionValues)
            .build();
        
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(filterExpression)
            .build();
        
        return Flux.from(eventTable.scan(scanRequest).items())
            .map(EntityMapper::toEvent);
    }
    
    @Override
    public Mono<Boolean> existsById(String eventId) {
        return findById(eventId)
            .map(event -> true)
            .defaultIfEmpty(false);
    }
    
    @Override
    public Mono<Void> deleteById(String eventId) {
        Key key = Key.builder()
            .partitionValue(eventId)
            .build();
        
        return Mono.fromCompletionStage(() -> eventTable.deleteItem(key))
            .then();
    }
    
    @Override
    public Mono<Event> updateInventory(String eventId, Integer expectedVersion, 
                                      Integer availableTickets, Integer reservedTickets) {
        return findById(eventId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found: " + eventId)))
            .flatMap(event -> {
                if (!event.version().equals(expectedVersion)) {
                    return Mono.error(new ConcurrentModificationException(
                        "Event", eventId, expectedVersion, event.version()));
                }
                
                Event updatedEvent = new Event(
                    event.eventId(),
                    event.name(),
                    event.date(),
                    event.location(),
                    event.totalCapacity(),
                    availableTickets,
                    reservedTickets,
                    event.complimentaryTickets(),
                    event.version() + 1,
                    event.createdAt(),
                    LocalDateTime.now()
                );
                
                return save(updatedEvent);
            });
    }
    
    @Override
    public Mono<Event> decrementAvailableTickets(String eventId, Integer expectedVersion, Integer quantity, Integer newAvailable, Integer newReserved) {
        
        return findById(eventId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found: " + eventId)))
            .flatMap(event -> {
                
                Event updatedEvent = new Event(
                    event.eventId(),
                    event.name(),
                    event.date(),
                    event.location(),
                    event.totalCapacity(),
                    newAvailable,
                    newReserved,
                    event.complimentaryTickets(),
                    expectedVersion + 1,
                    event.createdAt(),
                    LocalDateTime.now()
                );
                
                return saveWithCondition(updatedEvent, expectedVersion, quantity);
            });
    }
    
    private Mono<Event> saveWithCondition(Event event, Integer expectedVersion, Integer quantity) {
        EventEntity entity = EntityMapper.toEventEntity(event);
        
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":expectedVersion", AttributeValue.builder().n(String.valueOf(expectedVersion)).build());
        expressionValues.put(":quantity", AttributeValue.builder().n(String.valueOf(quantity)).build());
        
        Expression condition = Expression.builder()
            .expression("#version = :expectedVersion AND availableTickets >= :quantity")
            .expressionNames(Map.of("#version", "version"))
            .expressionValues(expressionValues)
            .build();
        
        PutItemEnhancedRequest<EventEntity> request = PutItemEnhancedRequest.builder(EventEntity.class)
            .item(entity)
            .conditionExpression(condition)
            .build();
        
        return Mono.fromCompletionStage(() -> eventTable.putItem(request))
            .thenReturn(event)
            .onErrorMap(ConditionalCheckFailedException.class, 
                ex -> new ConcurrentModificationException(
                    "Event", event.eventId(), expectedVersion, event.version()));
    }
}

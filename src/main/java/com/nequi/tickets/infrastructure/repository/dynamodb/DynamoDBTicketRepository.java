package com.nequi.tickets.infrastructure.repository.dynamodb;

import com.nequi.tickets.domain.exception.ConcurrentModificationException;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.infrastructure.repository.dynamodb.entity.TicketEntity;
import com.nequi.tickets.infrastructure.repository.dynamodb.mapper.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DynamoDBTicketRepository implements TicketRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBTicketRepository.class);
    
    private final DynamoDbEnhancedAsyncClient dynamoDbClient;
    private final DynamoDbAsyncTable<TicketEntity> ticketTable;
    private final DynamoDbAsyncIndex<TicketEntity> eventStatusIndex;
    private final DynamoDbAsyncIndex<TicketEntity> customerIndex;
    private final DynamoDbAsyncIndex<TicketEntity> orderIndex;
    
    public DynamoDBTicketRepository(
            DynamoDbEnhancedAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.tables.tickets}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.ticketTable = dynamoDbClient.table(tableName, TableSchema.fromBean(TicketEntity.class));
        this.eventStatusIndex = ticketTable.index("eventId-status-index");
        this.customerIndex = ticketTable.index("customerId-index");
        this.orderIndex = ticketTable.index("orderId-index");
    }
    
    @Override
    public Mono<Ticket> save(Ticket ticket) {
        TicketEntity entity = EntityMapper.toTicketEntity(ticket);
        
        return Mono.fromCompletionStage(() -> ticketTable.putItem(entity))
            .thenReturn(ticket)
            .onErrorMap(ConditionalCheckFailedException.class, 
                ex -> new ConcurrentModificationException(
                    "Ticket", ticket.ticketId()));
    }
    
    @Override
    public Flux<Ticket> saveAll(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Flux.empty();
        }
        
        List<TicketEntity> entities = tickets.stream()
            .map(EntityMapper::toTicketEntity)
            .toList();
        
        long startTime = System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicInteger batchCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        
        return Flux.fromIterable(partition(entities, 25))
            .flatMap(batch -> {
                
                WriteBatch.Builder<TicketEntity> batchBuilder = WriteBatch.builder(TicketEntity.class)
                    .mappedTableResource(ticketTable);
                
                batch.forEach(batchBuilder::addPutItem);
                
                BatchWriteItemEnhancedRequest request = BatchWriteItemEnhancedRequest.builder()
                    .writeBatches(batchBuilder.build())
                    .build();
                
                return Mono.fromCompletionStage(() -> dynamoDbClient.batchWriteItem(request));
                    
            }, 25)
            .thenMany(Flux.fromIterable(tickets));
    }
    
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> partitions = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
    
    @Override
    public Mono<Ticket> findById(String ticketId) {
        Key key = Key.builder()
            .partitionValue(ticketId)
            .build();
        
        return Mono.fromCompletionStage(() -> ticketTable.getItem(key))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Flux<Ticket> findByEventId(String eventId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(eventId).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(eventStatusIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Flux<Ticket> findByIds(List<String> ticketIds) {
        return Flux.fromIterable(ticketIds)
            .flatMap(this::findById)
            .onErrorContinue((error, item) -> {
            });
    }
    
    @Override
    public Flux<Ticket> findByEventIdAndStatus(String eventId, TicketStatus status) {
        QueryConditional queryConditional = QueryConditional
            .sortBeginsWith(Key.builder()
                .partitionValue(eventId)
                .sortValue(status.name())
                .build());
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(eventStatusIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Flux<Ticket> findByCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Flux.empty();
        }
        
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(customerId).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(customerIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Flux<Ticket> findByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Flux.empty();
        }
        
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(orderId).build()
        );
        
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();
        
        return Flux.from(orderIndex.query(queryRequest))
            .flatMap(page -> Flux.fromIterable(page.items()))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Flux<Ticket> findExpiredReservations(LocalDateTime expirationTime) {
        String expirationTimeStr = expirationTime.toString();
        
        logger.info("🔍 Scanning for expired reservations. Cutoff time: {} (ISO format)", expirationTimeStr);
        
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#status", "status");
        expressionNames.put("#expiresAt", "reservationExpiresAt");
        
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":statusPending", AttributeValue.builder().s(TicketStatus.PENDING_CONFIRMATION.name()).build());
        expressionValues.put(":expirationTime", AttributeValue.builder().s(expirationTimeStr).build());
        
        logger.info("🔎 Filter: (status = PENDING_CONFIRMATION) AND reservationExpiresAt < {}", expirationTimeStr);
        
        Expression filterExpression = Expression.builder()
            .expression("(#status = :statusPending) AND #expiresAt < :expirationTime")
            .expressionNames(expressionNames)
            .expressionValues(expressionValues)
            .build();
        
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(filterExpression)
            .build();
        
        return Flux.from(ticketTable.scan(scanRequest).items())
            .doOnNext(entity -> logger.info("📋 Found expired ticket: {} (status={}, expiresAt={})", 
                entity.getTicketId(), entity.getStatus(), entity.getReservationExpiresAt()))
            .doOnComplete(() -> logger.info("✅ Scan completed"))
            .map(EntityMapper::toTicket);
    }
    
    @Override
    public Mono<Long> countAvailableByEventId(String eventId) {
        return countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
    }
    
    @Override
    public Mono<Long> countByEventIdAndStatus(String eventId, TicketStatus status) {
        return findByEventIdAndStatus(eventId, status)
            .count();
    }
    
    @Override
    public Mono<Void> deleteById(String ticketId) {
        Key key = Key.builder()
            .partitionValue(ticketId)
            .build();
        
        return Mono.fromCompletionStage(() -> ticketTable.deleteItem(key))
            .then();
    }
    
    @Override
    public Mono<Void> deleteByEventId(String eventId) {
        return findByEventId(eventId)
            .flatMap(ticket -> deleteById(ticket.ticketId()))
            .then();
    }
}

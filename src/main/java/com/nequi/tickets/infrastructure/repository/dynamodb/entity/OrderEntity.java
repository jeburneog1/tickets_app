package com.nequi.tickets.infrastructure.repository.dynamodb.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@DynamoDbBean
public class OrderEntity {
    
    private String orderId;
    private String eventId;
    private String customerId;
    private List<String> ticketIds;
    private String status;
    private Integer totalTickets;
    private Integer retryCount;
    private Integer version;
    private String failureReason;
    private String createdAt;
    private String updatedAt;
    private String processedAt;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("orderId")
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "eventId-index")
    @DynamoDbAttribute("eventId")
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "customerId-index")
    @DynamoDbAttribute("customerId")
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    @DynamoDbAttribute("ticketIds")
    public List<String> getTicketIds() {
        return ticketIds;
    }
    
    public void setTicketIds(List<String> ticketIds) {
        this.ticketIds = ticketIds;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @DynamoDbAttribute("totalTickets")
    public Integer getTotalTickets() {
        return totalTickets;
    }
    
    public void setTotalTickets(Integer totalTickets) {
        this.totalTickets = totalTickets;
    }
    
    @DynamoDbAttribute("retryCount")
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    @DynamoDbAttribute("version")
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    @DynamoDbAttribute("failureReason")
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @DynamoDbAttribute("updatedAt")
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @DynamoDbAttribute("processedAt")
    public String getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}

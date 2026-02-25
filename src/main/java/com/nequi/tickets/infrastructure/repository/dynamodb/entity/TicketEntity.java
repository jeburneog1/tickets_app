package com.nequi.tickets.infrastructure.repository.dynamodb.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class TicketEntity {
    
    private String ticketId;
    private String eventId;
    private String status;
    private String customerId;
    private String orderId;
    private String reservedAt;
    private String reservationExpiresAt;
    private Integer version;
    private String createdAt;
    private String updatedAt;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("ticketId")
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "eventId-status-index")
    @DynamoDbAttribute("eventId")
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    @DynamoDbSecondarySortKey(indexNames = "eventId-status-index")
    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "customerId-index")
    @DynamoDbAttribute("customerId")
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "orderId-index")
    @DynamoDbAttribute("orderId")
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    @DynamoDbAttribute("reservedAt")
    public String getReservedAt() {
        return reservedAt;
    }
    
    public void setReservedAt(String reservedAt) {
        this.reservedAt = reservedAt;
    }
    
    @DynamoDbAttribute("reservationExpiresAt")
    public String getReservationExpiresAt() {
        return reservationExpiresAt;
    }
    
    public void setReservationExpiresAt(String reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }
    
    @DynamoDbAttribute("version")
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
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
}

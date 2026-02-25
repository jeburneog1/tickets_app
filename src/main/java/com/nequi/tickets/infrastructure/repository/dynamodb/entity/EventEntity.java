package com.nequi.tickets.infrastructure.repository.dynamodb.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;

@DynamoDbBean
public class EventEntity {
    
    private String eventId;
    private String name;
    private String date;
    private String location;
    private Integer totalCapacity;
    private Integer availableTickets;
    private Integer reservedTickets;
    private Integer complimentaryTickets;
    private Integer version;
    private String createdAt;
    private String updatedAt;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("eventId")
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @DynamoDbAttribute("date")
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    @DynamoDbAttribute("location")
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @DynamoDbAttribute("totalCapacity")
    public Integer getTotalCapacity() {
        return totalCapacity;
    }
    
    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    
    @DynamoDbAttribute("availableTickets")
    public Integer getAvailableTickets() {
        return availableTickets;
    }
    
    public void setAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
    }
    
    @DynamoDbAttribute("reservedTickets")
    public Integer getReservedTickets() {
        return reservedTickets;
    }
    
    public void setReservedTickets(Integer reservedTickets) {
        this.reservedTickets = reservedTickets;
    }
    
    @DynamoDbAttribute("complimentaryTickets")
    public Integer getComplimentaryTickets() {
        return complimentaryTickets;
    }
    
    public void setComplimentaryTickets(Integer complimentaryTickets) {
        this.complimentaryTickets = complimentaryTickets;
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
    
    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
    
    public static LocalDateTime fromIsoString(String isoString) {
        return isoString != null ? LocalDateTime.parse(isoString) : null;
    }
}

package com.nequi.tickets.domain.exception;

public class ConcurrentModificationException extends DomainException {
    
    private final String entityType;
    private final String entityId;
    private final Integer expectedVersion;
    private final Integer actualVersion;
    
    public ConcurrentModificationException(
        String entityType,
        String entityId,
        Integer expectedVersion,
        Integer actualVersion
    ) {
        super(String.format(
            "Concurrent modification detected for %s with ID %s. Expected version: %d, Actual version: %d. Please retry.",
            entityType, entityId, expectedVersion, actualVersion
        ));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public ConcurrentModificationException(String entityType, String entityId) {
        super(String.format(
            "Concurrent modification detected for %s with ID %s. Please retry.",
            entityType, entityId
        ));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public Integer getExpectedVersion() {
        return expectedVersion;
    }
    
    public Integer getActualVersion() {
        return actualVersion;
    }
}

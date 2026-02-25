package com.nequi.tickets.domain.exception;

public class InvalidStateTransitionException extends DomainException {
    
    private final String entityType;
    private final String entityId;
    private final String currentState;
    private final String attemptedTransition;
    
    public InvalidStateTransitionException(
        String entityType,
        String entityId,
        String currentState,
        String attemptedTransition
    ) {
        super(String.format(
            "Invalid state transition for %s %s. Current state: %s. Attempted: %s",
            entityType, entityId, currentState, attemptedTransition
        ));
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentState = currentState;
        this.attemptedTransition = attemptedTransition;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public String getAttemptedTransition() {
        return attemptedTransition;
    }
}

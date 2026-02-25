package com.nequi.tickets.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderStatus Enum Tests")
class OrderStatusTest {
    @Test
    @DisplayName("Should identify final states correctly")
    void shouldIdentifyFinalStates() {
        assertTrue(OrderStatus.CONFIRMED.isFinalState());
        assertTrue(OrderStatus.FAILED.isFinalState());
        assertTrue(OrderStatus.CANCELLED.isFinalState());
        assertFalse(OrderStatus.PENDING.isFinalState());
        assertFalse(OrderStatus.PROCESSING.isFinalState());
    }
    @Test
    @DisplayName("Should identify successful orders correctly")
    void shouldIdentifySuccessfulOrders() {
        assertTrue(OrderStatus.CONFIRMED.isSuccessful());
        assertFalse(OrderStatus.FAILED.isSuccessful());
        assertFalse(OrderStatus.CANCELLED.isSuccessful());
        assertFalse(OrderStatus.PENDING.isSuccessful());
        assertFalse(OrderStatus.PROCESSING.isSuccessful());
    }
    @Test
    @DisplayName("Should identify active orders correctly")
    void shouldIdentifyActiveOrders() {
        assertTrue(OrderStatus.PENDING.isActive());
        assertTrue(OrderStatus.PROCESSING.isActive());
        assertFalse(OrderStatus.CONFIRMED.isActive());
        assertFalse(OrderStatus.FAILED.isActive());
        assertFalse(OrderStatus.CANCELLED.isActive());
    }
    @Test
    @DisplayName("Should identify processable orders correctly")
    void shouldIdentifyProcessableOrders() {
        assertTrue(OrderStatus.PENDING.canBeProcessed());
        assertFalse(OrderStatus.PROCESSING.canBeProcessed());
        assertFalse(OrderStatus.CONFIRMED.canBeProcessed());
        assertFalse(OrderStatus.FAILED.canBeProcessed());
        assertFalse(OrderStatus.CANCELLED.canBeProcessed());
    }
    @Test
    @DisplayName("Should identify cancellable orders correctly")
    void shouldIdentifyCancellableOrders() {
        assertTrue(OrderStatus.PENDING.canBeCancelled());
        assertTrue(OrderStatus.PROCESSING.canBeCancelled());
        assertFalse(OrderStatus.CONFIRMED.canBeCancelled());
        assertFalse(OrderStatus.FAILED.canBeCancelled());
        assertFalse(OrderStatus.CANCELLED.canBeCancelled());
    }
    @Test
    @DisplayName("Should have all expected status values")
    void shouldHaveAllExpectedStatusValues() {
        OrderStatus[] allStatuses = OrderStatus.values();
        assertEquals(5, allStatuses.length);
        assertArrayEquals(
            new OrderStatus[]{
                OrderStatus.PENDING,
                OrderStatus.PROCESSING,
                OrderStatus.CONFIRMED,
                OrderStatus.FAILED,
                OrderStatus.CANCELLED
            },
            allStatuses
        );
    }
    @Test
    @DisplayName("Should convert string to enum correctly")
    void shouldConvertStringToEnum() {
        assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
        assertEquals(OrderStatus.PROCESSING, OrderStatus.valueOf("PROCESSING"));
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.valueOf("CONFIRMED"));
        assertEquals(OrderStatus.FAILED, OrderStatus.valueOf("FAILED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }
    @Test
    @DisplayName("Should throw exception for invalid status string")
    void shouldThrowExceptionForInvalidStatusString() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.valueOf("INVALID"));
    }
    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("Should have valid toString for all statuses")
    void shouldHaveValidToStringForAllStatuses(OrderStatus status) {
        assertNotNull(status.toString());
        assertFalse(status.toString().isEmpty());
    }
    @Test
    @DisplayName("Should validate business rule: Three final states exist")
    void shouldValidateFinalStatesCount() {
        int finalStatesCount = 0;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isFinalState()) {
                finalStatesCount++;
            }
        }
        assertEquals(3, finalStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Only one successful outcome")
    void shouldValidateOnlyOneSuccessfulOutcome() {
        int successfulStatesCount = 0;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isSuccessful()) {
                successfulStatesCount++;
            }
        }
        assertEquals(1, successfulStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Two active states")
    void shouldValidateTwoActiveStates() {
        int activeStatesCount = 0;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isActive()) {
                activeStatesCount++;
            }
        }
        assertEquals(2, activeStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Only PENDING can be processed")
    void shouldValidateOnlyPendingCanBeProcessed() {
        int processableStatesCount = 0;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.canBeProcessed()) {
                processableStatesCount++;
            }
        }
        assertEquals(1, processableStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Active states can be cancelled")
    void shouldValidateActiveStatesCanBeCancelled() {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isActive()) {
                assertTrue(status.canBeCancelled(), 
                    "Active status " + status + " should be cancellable");
            }
        }
    }
    @Test
    @DisplayName("Should validate business rule: Final states cannot be cancelled")
    void shouldValidateFinalStatesCannotBeCancelled() {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isFinalState()) {
                assertFalse(status.canBeCancelled(), 
                    "Final status " + status + " should not be cancellable");
            }
        }
    }
    @Test
    @DisplayName("Should validate business rule: PENDING is first state")
    void shouldValidatePendingIsFirstState() {
        assertEquals(OrderStatus.PENDING, OrderStatus.values()[0]);
        assertTrue(OrderStatus.PENDING.canBeProcessed());
    }
    @Test
    @DisplayName("Should validate state transition: PENDING to PROCESSING")
    void shouldValidateTransitionPendingToProcessing() {
        OrderStatus pending = OrderStatus.PENDING;
        assertTrue(pending.canBeProcessed());
        assertFalse(pending.isFinalState());
        assertTrue(pending.isActive());
    }
    @Test
    @DisplayName("Should validate state transition: PROCESSING to terminal states")
    void shouldValidateTransitionProcessingToTerminalStates() {
        OrderStatus processing = OrderStatus.PROCESSING;
        assertFalse(processing.canBeProcessed());
        assertFalse(processing.isFinalState());
        assertTrue(processing.isActive());
        assertTrue(processing.canBeCancelled());
    }
}
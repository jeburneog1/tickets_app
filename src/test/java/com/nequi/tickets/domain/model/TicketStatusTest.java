package com.nequi.tickets.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TicketStatus Enum Tests")
class TicketStatusTest {
    @Test
    @DisplayName("Should identify final states correctly")
    void shouldIdentifyFinalStates() {
        assertTrue(TicketStatus.SOLD.isFinalState());
        assertTrue(TicketStatus.COMPLIMENTARY.isFinalState());
        assertFalse(TicketStatus.AVAILABLE.isFinalState());
        assertFalse(TicketStatus.RESERVED.isFinalState());
        assertFalse(TicketStatus.PENDING_CONFIRMATION.isFinalState());
    }
    @Test
    @DisplayName("Should identify unavailable tickets correctly")
    void shouldIdentifyUnavailableTickets() {
        assertFalse(TicketStatus.AVAILABLE.isUnavailable());
        assertTrue(TicketStatus.RESERVED.isUnavailable());
        assertTrue(TicketStatus.PENDING_CONFIRMATION.isUnavailable());
        assertTrue(TicketStatus.SOLD.isUnavailable());
        assertTrue(TicketStatus.COMPLIMENTARY.isUnavailable());
    }
    @Test
    @DisplayName("Should identify sold tickets correctly")
    void shouldIdentifySoldTickets() {
        assertTrue(TicketStatus.SOLD.isSold());
        assertFalse(TicketStatus.COMPLIMENTARY.isSold());
        assertFalse(TicketStatus.AVAILABLE.isSold());
        assertFalse(TicketStatus.RESERVED.isSold());
        assertFalse(TicketStatus.PENDING_CONFIRMATION.isSold());
    }
    @Test
    @DisplayName("Should identify temporary statuses correctly")
    void shouldIdentifyTemporaryStatuses() {
        assertTrue(TicketStatus.RESERVED.isTemporary());
        assertTrue(TicketStatus.PENDING_CONFIRMATION.isTemporary());
        assertFalse(TicketStatus.AVAILABLE.isTemporary());
        assertFalse(TicketStatus.SOLD.isTemporary());
        assertFalse(TicketStatus.COMPLIMENTARY.isTemporary());
    }
    @Test
    @DisplayName("Should have all expected status values")
    void shouldHaveAllExpectedStatusValues() {
        TicketStatus[] allStatuses = TicketStatus.values();
        assertEquals(5, allStatuses.length);
        assertArrayEquals(
            new TicketStatus[]{
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                TicketStatus.PENDING_CONFIRMATION,
                TicketStatus.SOLD,
                TicketStatus.COMPLIMENTARY
            },
            allStatuses
        );
    }
    @Test
    @DisplayName("Should convert string to enum correctly")
    void shouldConvertStringToEnum() {
        assertEquals(TicketStatus.AVAILABLE, TicketStatus.valueOf("AVAILABLE"));
        assertEquals(TicketStatus.RESERVED, TicketStatus.valueOf("RESERVED"));
        assertEquals(TicketStatus.PENDING_CONFIRMATION, TicketStatus.valueOf("PENDING_CONFIRMATION"));
        assertEquals(TicketStatus.SOLD, TicketStatus.valueOf("SOLD"));
        assertEquals(TicketStatus.COMPLIMENTARY, TicketStatus.valueOf("COMPLIMENTARY"));
    }
    @Test
    @DisplayName("Should throw exception for invalid status string")
    void shouldThrowExceptionForInvalidStatusString() {
        assertThrows(IllegalArgumentException.class, () -> TicketStatus.valueOf("INVALID"));
    }
    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    @DisplayName("Should have valid toString for all statuses")
    void shouldHaveValidToStringForAllStatuses(TicketStatus status) {
        assertNotNull(status.toString());
        assertFalse(status.toString().isEmpty());
    }
    @Test
    @DisplayName("Should validate business rule: SOLD and COMPLIMENTARY are exclusive final states")
    void shouldValidateFinalStatesAreExclusive() {
        int finalStatesCount = 0;
        for (TicketStatus status : TicketStatus.values()) {
            if (status.isFinalState()) {
                finalStatesCount++;
            }
        }
        assertEquals(2, finalStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Temporary states can expire")
    void shouldValidateTemporaryStatesLogic() {
        int temporaryStatesCount = 0;
        for (TicketStatus status : TicketStatus.values()) {
            if (status.isTemporary()) {
                temporaryStatesCount++;
            }
        }
        assertEquals(2, temporaryStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Only SOLD counts as actual sale")
    void shouldValidateOnlySoldCountsAsSale() {
        int soldStatesCount = 0;
        for (TicketStatus status : TicketStatus.values()) {
            if (status.isSold()) {
                soldStatesCount++;
            }
        }
        assertEquals(1, soldStatesCount);
    }
    @Test
    @DisplayName("Should validate business rule: Only AVAILABLE tickets can be purchased")
    void shouldValidateOnlyAvailableCanBePurchased() {
        int availableStatesCount = 0;
        for (TicketStatus status : TicketStatus.values()) {
            if (!status.isUnavailable()) {
                availableStatesCount++;
            }
        }
        assertEquals(1, availableStatesCount);
        assertTrue(TicketStatus.AVAILABLE == TicketStatus.values()[0]);
    }
}
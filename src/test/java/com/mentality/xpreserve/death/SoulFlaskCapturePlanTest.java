package com.mentality.xpreserve.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoulFlaskCapturePlanTest {
    @Test
    void capturesOnlyAvailableCapacity() {
        SoulFlaskCapturePlan plan = SoulFlaskCapturePlan.create(5_000, 30_000, 30_970);
        assertEquals(970, plan.capturedXp());
        assertEquals(30_970, plan.resultingStoredXp());
        assertEquals(4_030, plan.remainingPlayerXp());
    }

    @Test
    void fullOrEmptyPlayerDoesNotTrigger() {
        assertFalse(SoulFlaskCapturePlan.create(1_000, 30_970, 30_970).capturedAnything());
        assertFalse(SoulFlaskCapturePlan.create(0, 0, 30_970).capturedAnything());
    }

    @Test
    void invalidStoredStateFailsClosed() {
        SoulFlaskCapturePlan plan = SoulFlaskCapturePlan.create(1_000, 30_971, 30_970);
        assertFalse(plan.capturedAnything());
        assertEquals(1_000, plan.remainingPlayerXp());
    }

    @Test
    void onlyOneTransactionUsesThePlayerXp() {
        SoulFlaskCapturePlan plan = SoulFlaskCapturePlan.create(40_000, 0, 30_970);
        assertTrue(plan.capturedAnything());
        assertEquals(30_970, plan.capturedXp());
        assertEquals(9_030, plan.remainingPlayerXp());
    }
}

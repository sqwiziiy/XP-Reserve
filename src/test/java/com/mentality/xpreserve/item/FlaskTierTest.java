package com.mentality.xpreserve.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mentality.xpreserve.xp.XpMath;
import com.mentality.xpreserve.xp.XpTransaction;
import com.mentality.xpreserve.xp.XpTransfer;
import org.junit.jupiter.api.Test;

class FlaskTierTest {
    @Test
    void capacitiesAreCanonicalAndStrictlyMonotonic() {
        assertEquals(XpMath.totalXpAtLevel(30), FlaskTier.I.capacity());
        assertEquals(XpMath.totalXpAtLevel(50), FlaskTier.II.capacity());
        assertEquals(XpMath.totalXpAtLevel(100), FlaskTier.III.capacity());
        assertEquals(1395, FlaskTier.I.capacity());
        assertEquals(5345, FlaskTier.II.capacity());
        assertEquals(30970, FlaskTier.III.capacity());
        assertTrue(FlaskTier.I.capacity() < FlaskTier.II.capacity());
        assertTrue(FlaskTier.II.capacity() < FlaskTier.III.capacity());
    }

    @Test
    void everyTierConservesExactRawXp() {
        for (FlaskTier tier : FlaskTier.values()) {
            XpTransaction belowCapacity = XpTransfer.deposit(tier.capacity() - 1, 0, tier.capacity());
            assertEquals(0, belowCapacity.newPlayerRawXp());
            assertEquals(tier.capacity() - 1, belowCapacity.newStoredRawXp());

            long aboveCapacity = tier.capacity() + 2000;
            XpTransaction deposit = XpTransfer.deposit(aboveCapacity, 0, tier.capacity());
            assertEquals(2000, deposit.newPlayerRawXp());
            assertEquals(tier.capacity(), deposit.newStoredRawXp());

            XpTransaction partial = XpTransfer.deposit(100, tier.capacity() - 40, tier.capacity());
            assertEquals(60, partial.newPlayerRawXp());
            assertEquals(tier.capacity(), partial.newStoredRawXp());

            XpTransaction fullNoOp = XpTransfer.deposit(100, tier.capacity(), tier.capacity());
            assertEquals(100, fullNoOp.newPlayerRawXp());
            assertEquals(tier.capacity(), fullNoOp.newStoredRawXp());

            XpTransaction withdrawal = XpTransfer.withdraw(deposit.newPlayerRawXp(), deposit.newStoredRawXp(), tier.capacity());
            assertEquals(aboveCapacity, withdrawal.newPlayerRawXp() + withdrawal.newStoredRawXp());
            assertEquals(0, withdrawal.newStoredRawXp());
        }
    }
}

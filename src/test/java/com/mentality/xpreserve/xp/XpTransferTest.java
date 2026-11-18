package com.mentality.xpreserve.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class XpTransferTest {
    private static final long CAPACITY = XpMath.totalXpAtLevel(30);

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0, 0, 0",
            "100, 0, 100, 0, 100",
            "1395, 0, 1395, 0, 1395",
            "2000, 0, 1395, 605, 1395",
            "500, 1000, 395, 105, 1395",
            "1000, 1395, 0, 1000, 1395"
    })
    void depositsRespectCapacityAndConservation(
            long player, long stored, long transfer, long expectedPlayer, long expectedStored) {
        XpTransaction transaction = XpTransfer.deposit(player, stored, CAPACITY);

        assertTransaction(transaction, player, stored, transfer, expectedPlayer, expectedStored);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0, 0, 0",
            "0, 1, 1, 1, 0",
            "0, 1395, 1395, 1395, 0",
            "1000, 395, 395, 1395, 0"
    })
    void withdrawalsReturnEveryStoredPoint(
            long player, long stored, long transfer, long expectedPlayer, long expectedStored) {
        XpTransaction transaction = XpTransfer.withdraw(player, stored, CAPACITY);

        assertTransaction(transaction, player, stored, transfer, expectedPlayer, expectedStored);
    }

    @org.junit.jupiter.api.Test
    void repeatedDepositWithdrawCyclesConserveRawXp() {
        long player = 50_000;
        long stored = 0;
        long initialTotal = player + stored;

        for (int cycle = 0; cycle < 1_000; cycle++) {
            XpTransaction deposit = XpTransfer.deposit(player, stored, CAPACITY);
            player = deposit.newPlayerRawXp();
            stored = deposit.newStoredRawXp();

            XpTransaction withdrawal = XpTransfer.withdraw(player, stored, CAPACITY);
            player = withdrawal.newPlayerRawXp();
            stored = withdrawal.newStoredRawXp();
        }

        assertEquals(initialTotal, player + stored);
        assertEquals(0, stored);
    }

    @org.junit.jupiter.api.Test
    void partialCycleOnlyChangesForSimulatedExternalXp() {
        long player = 2_000;
        long stored = 1_000;
        long initialTotal = player + stored;

        XpTransaction firstDeposit = XpTransfer.deposit(player, stored, CAPACITY);
        player = firstDeposit.newPlayerRawXp();
        stored = firstDeposit.newStoredRawXp();

        player += 250; // Explicitly simulated external XP gain.

        XpTransaction secondDeposit = XpTransfer.deposit(player, stored, CAPACITY);
        XpTransaction withdrawal = XpTransfer.withdraw(
                secondDeposit.newPlayerRawXp(), secondDeposit.newStoredRawXp(), CAPACITY);

        assertEquals(initialTotal + 250, withdrawal.newPlayerRawXp() + withdrawal.newStoredRawXp());
        assertEquals(0, withdrawal.newStoredRawXp());
    }

    @org.junit.jupiter.api.Test
    void invalidInputsAndOverflowFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> XpTransfer.deposit(-1, 0, CAPACITY));
        assertThrows(IllegalArgumentException.class, () -> XpTransfer.deposit(0, -1, CAPACITY));
        assertThrows(IllegalArgumentException.class, () -> XpTransfer.deposit(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> XpTransfer.deposit(0, CAPACITY + 1, CAPACITY));
        assertThrows(ArithmeticException.class, () -> XpTransfer.withdraw(Long.MAX_VALUE, 1, CAPACITY));
    }

    private static void assertTransaction(
            XpTransaction transaction,
            long oldPlayer,
            long oldStored,
            long transferred,
            long newPlayer,
            long newStored) {
        assertEquals(oldPlayer, transaction.oldPlayerRawXp());
        assertEquals(oldStored, transaction.oldStoredRawXp());
        assertEquals(transferred, transaction.transferredRawXp());
        assertEquals(newPlayer, transaction.newPlayerRawXp());
        assertEquals(newStored, transaction.newStoredRawXp());
        assertEquals(oldPlayer + oldStored, transaction.newPlayerRawXp() + transaction.newStoredRawXp());
        assertEquals(transferred > 0, transaction.changed());
        if (transferred == 0) {
            assertFalse(transaction.changed());
        } else {
            assertTrue(transaction.changed());
        }
    }
}

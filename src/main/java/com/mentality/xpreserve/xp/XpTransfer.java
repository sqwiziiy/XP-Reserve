package com.mentality.xpreserve.xp;

/** Pure raw-XP deposit and withdrawal calculations for XP storage items. */
public final class XpTransfer {
    private XpTransfer() {
    }

    public static XpTransaction deposit(long playerRawXp, long storedRawXp, long capacity) {
        validateState(playerRawXp, storedRawXp, capacity);
        long transferred = Math.min(playerRawXp, capacity - storedRawXp);
        return new XpTransaction(
                playerRawXp,
                storedRawXp,
                transferred,
                playerRawXp - transferred,
                Math.addExact(storedRawXp, transferred)
        );
    }

    public static XpTransaction withdraw(long playerRawXp, long storedRawXp, long capacity) {
        validateState(playerRawXp, storedRawXp, capacity);
        return new XpTransaction(
                playerRawXp,
                storedRawXp,
                storedRawXp,
                Math.addExact(playerRawXp, storedRawXp),
                0
        );
    }

    private static void validateState(long playerRawXp, long storedRawXp, long capacity) {
        if (playerRawXp < 0) {
            throw new IllegalArgumentException("Player raw XP must not be negative: " + playerRawXp);
        }
        if (storedRawXp < 0) {
            throw new IllegalArgumentException("Stored raw XP must not be negative: " + storedRawXp);
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must not be negative: " + capacity);
        }
        if (storedRawXp > capacity) {
            throw new IllegalArgumentException("Stored raw XP must not exceed capacity");
        }
    }
}

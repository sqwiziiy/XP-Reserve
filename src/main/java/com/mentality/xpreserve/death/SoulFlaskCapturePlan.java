package com.mentality.xpreserve.death;

/** Pure arithmetic for the one Soul Flask capture transaction. */
public record SoulFlaskCapturePlan(long capturedXp, long resultingStoredXp, long remainingPlayerXp) {
    public static SoulFlaskCapturePlan create(long playerXp, long storedXp, long capacity) {
        if (playerXp < 0 || storedXp < 0 || storedXp > capacity) {
            return new SoulFlaskCapturePlan(0, storedXp, playerXp);
        }
        long captured = Math.min(playerXp, capacity - storedXp);
        return new SoulFlaskCapturePlan(captured, storedXp + captured, playerXp - captured);
    }

    public boolean capturedAnything() {
        return capturedXp > 0;
    }
}

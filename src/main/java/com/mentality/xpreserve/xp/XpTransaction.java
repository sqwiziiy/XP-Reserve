package com.mentality.xpreserve.xp;

/** Immutable, conservation-checked result of one raw-XP transfer. */
public record XpTransaction(
        long oldPlayerRawXp,
        long oldStoredRawXp,
        long transferredRawXp,
        long newPlayerRawXp,
        long newStoredRawXp
) {
    public XpTransaction {
        if (oldPlayerRawXp < 0 || oldStoredRawXp < 0 || transferredRawXp < 0
                || newPlayerRawXp < 0 || newStoredRawXp < 0) {
            throw new IllegalArgumentException("Raw XP transaction values must not be negative");
        }
        if (Math.addExact(oldPlayerRawXp, oldStoredRawXp)
                != Math.addExact(newPlayerRawXp, newStoredRawXp)) {
            throw new IllegalArgumentException("Raw XP transfer must conserve total XP");
        }
    }

    public boolean changed() {
        return transferredRawXp > 0;
    }
}

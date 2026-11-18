package com.mentality.xpreserve.xp;

/**
 * Immutable, lossless decomposition of a raw XP total into Minecraft level progress.
 *
 * @param totalXp raw XP points from level zero
 * @param level current experience level
 * @param xpIntoLevel raw XP earned after entering {@code level}
 * @param xpNeededForNextLevel raw XP required to enter the next level
 */
public record XpState(long totalXp, int level, long xpIntoLevel, long xpNeededForNextLevel) {
    public XpState {
        if (totalXp < 0) {
            throw new IllegalArgumentException("Total XP must not be negative: " + totalXp);
        }
        if (level < 0) {
            throw new IllegalArgumentException("Level must not be negative: " + level);
        }
        if (xpIntoLevel < 0 || xpIntoLevel >= xpNeededForNextLevel) {
            throw new IllegalArgumentException("XP within level must be in [0, xpNeededForNextLevel)");
        }
        if (xpNeededForNextLevel <= 0) {
            throw new IllegalArgumentException("XP needed for next level must be positive");
        }
        if (totalXp != Math.addExact(XpMath.totalXpAtLevel(level), xpIntoLevel)) {
            throw new IllegalArgumentException("XP state does not reconstruct its raw total exactly");
        }
    }

    /** Returns progress through the current level as a derived presentation value. */
    public double normalizedProgress() {
        return (double) xpIntoLevel / xpNeededForNextLevel;
    }

    /** Returns the original raw XP total without rounding or conversion loss. */
    public long toTotalXp() {
        return totalXp;
    }
}

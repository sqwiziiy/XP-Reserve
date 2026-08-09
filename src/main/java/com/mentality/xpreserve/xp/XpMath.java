package com.mentality.xpreserve.xp;

import java.math.BigInteger;

/**
 * Authoritative, integer-exact conversions for Minecraft experience.
 *
 * <p>All XP quantities represented by this class are raw XP points. Levels are
 * derived presentation/progression values and must never be used as a stored
 * currency. Invalid negative inputs are rejected. Results that cannot fit the
 * declared return type fail explicitly rather than wrapping or clamping.</p>
 */
public final class XpMath {
    private static final BigInteger TWO = BigInteger.TWO;

    private XpMath() {
    }

    /**
     * Returns the raw XP needed to advance from {@code level} to {@code level + 1}.
     *
     * @throws IllegalArgumentException if {@code level} is negative
     * @throws ArithmeticException if the result does not fit in an {@code int}
     */
    public static int xpNeededForNextLevel(int level) {
        return Math.toIntExact(xpNeededForNextLevelAsLong(level));
    }

    /**
     * Returns the exact total raw XP required to reach {@code level} from level zero.
     *
     * @throws IllegalArgumentException if {@code level} is negative
     * @throws ArithmeticException if the result does not fit in a {@code long}
     */
    public static long totalXpAtLevel(int level) {
        requireNonNegativeLevel(level);

        try {
            return totalXpAtLevelAsBigInteger(level).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ArithmeticException("Total XP at level " + level + " does not fit in a long");
        }
    }

    /**
     * Returns the highest level whose starting total is not greater than {@code totalXp}.
     *
     * @throws IllegalArgumentException if {@code totalXp} is negative
     */
    public static int levelFromTotalXp(long totalXp) {
        requireNonNegativeTotalXp(totalXp);

        int lower = 0;
        int upper = Integer.MAX_VALUE;
        while (lower < upper) {
            int candidate = lower + (int) (((long) upper - lower + 1) / 2);
            if (isLevelStartAtMost(candidate, totalXp)) {
                lower = candidate;
            } else {
                upper = candidate - 1;
            }
        }
        return lower;
    }

    /**
     * Returns the raw XP earned after entering the current level.
     *
     * @throws IllegalArgumentException if {@code totalXp} is negative
     */
    public static long xpIntoLevel(long totalXp) {
        requireNonNegativeTotalXp(totalXp);
        return totalXp - totalXpAtLevel(levelFromTotalXp(totalXp));
    }

    /**
     * Derives a complete, lossless level/progress representation from raw XP.
     *
     * @throws IllegalArgumentException if {@code totalXp} is negative
     */
    public static XpState stateFromTotalXp(long totalXp) {
        requireNonNegativeTotalXp(totalXp);
        int level = levelFromTotalXp(totalXp);
        long xpIntoLevel = totalXp - totalXpAtLevel(level);
        return new XpState(totalXp, level, xpIntoLevel, xpNeededForNextLevelAsLong(level));
    }

    /**
     * Reconstructs raw XP from vanilla's visible level/progress representation.
     * Vanilla stores progress as the float produced by {@code points / needed}; selecting the
     * nearest valid integer point reverses that conversion without systematic rounding drift.
     */
    public static long totalXpFromLevelProgress(int level, float progress) {
        requireNonNegativeLevel(level);
        if (!Float.isFinite(progress) || progress < 0.0F || progress >= 1.0F) {
            throw new IllegalArgumentException("Vanilla experience progress must be finite and in [0, 1): " + progress);
        }
        int needed = xpNeededForNextLevel(level);
        int nearestPoint = Math.round(progress * needed);
        int intoLevel = Math.min(needed - 1, Math.max(0, nearestPoint));
        return Math.addExact(totalXpAtLevel(level), intoLevel);
    }

    static long xpNeededForNextLevelAsLong(int level) {
        requireNonNegativeLevel(level);
        if (level <= 15) {
            return Math.addExact(Math.multiplyExact(2L, level), 7L);
        }
        if (level <= 30) {
            return Math.subtractExact(Math.multiplyExact(5L, level), 38L);
        }
        return Math.subtractExact(Math.multiplyExact(9L, level), 158L);
    }

    private static boolean isLevelStartAtMost(int level, long totalXp) {
        try {
            return totalXpAtLevel(level) <= totalXp;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private static BigInteger totalXpAtLevelAsBigInteger(int level) {
        BigInteger levelValue = BigInteger.valueOf(level);
        if (level <= 16) {
            return levelValue.multiply(levelValue).add(levelValue.multiply(BigInteger.valueOf(6)));
        }
        if (level <= 31) {
            return levelValue.multiply(levelValue).multiply(BigInteger.valueOf(5))
                    .subtract(levelValue.multiply(BigInteger.valueOf(81)))
                    .add(BigInteger.valueOf(720))
                    .divide(TWO);
        }
        return levelValue.multiply(levelValue).multiply(BigInteger.valueOf(9))
                .subtract(levelValue.multiply(BigInteger.valueOf(325)))
                .add(BigInteger.valueOf(4440))
                .divide(TWO);
    }

    private static void requireNonNegativeLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must not be negative: " + level);
        }
    }

    private static void requireNonNegativeTotalXp(long totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("Total XP must not be negative: " + totalXp);
        }
    }
}

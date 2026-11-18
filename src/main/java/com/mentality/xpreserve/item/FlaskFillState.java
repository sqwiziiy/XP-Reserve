package com.mentality.xpreserve.item;

/** Presentation-only fill state derived from authoritative raw XP. */
public enum FlaskFillState {
    EMPTY, LOW, MEDIUM, HIGH, FULL;

    public static FlaskFillState from(long storedXp, long capacity) {
        validate(storedXp, capacity);
        if (storedXp == 0) return EMPTY;
        if (storedXp == capacity) return FULL;
        if (Math.multiplyExact(storedXp, 4) <= capacity) return LOW;
        if (Math.multiplyExact(storedXp, 2) <= capacity) return MEDIUM;
        return HIGH;
    }

    /** Rounded presentation percentage; partial stacks remain visibly below 100%. */
    public static int percentage(long storedXp, long capacity) {
        validate(storedXp, capacity);
        if (storedXp == 0) return 0;
        if (storedXp == capacity) return 100;
        long rounded = Math.divideExact(Math.addExact(Math.multiplyExact(storedXp, 100), capacity / 2), capacity);
        return (int) Math.clamp(rounded, 1, 99);
    }

    public static float fraction(long storedXp, long capacity) {
        validate(storedXp, capacity);
        return (float) ((double) storedXp / capacity);
    }

    private static void validate(long storedXp, long capacity) {
        if (capacity <= 0 || storedXp < 0 || storedXp > capacity) {
            throw new IllegalArgumentException("Invalid flask visual state: stored=" + storedXp + ", capacity=" + capacity);
        }
    }
}

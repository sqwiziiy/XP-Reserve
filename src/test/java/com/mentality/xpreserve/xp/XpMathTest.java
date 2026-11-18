package com.mentality.xpreserve.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.LongStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class XpMathTest {
    @ParameterizedTest
    @CsvSource({
            "0, 0", "1, 7", "15, 315", "16, 352", "17, 394",
            "30, 1395", "31, 1507", "32, 1628", "50, 5345", "100, 30970"
    })
    void totalXpAtLevelMatchesKnownMinecraftValues(int level, long expectedTotalXp) {
        assertEquals(expectedTotalXp, XpMath.totalXpAtLevel(level));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 7", "15, 37", "16, 42", "30, 112", "31, 121", "32, 130"
    })
    void xpNeededForNextLevelMatchesVanillaCurveAtBoundaries(int level, int expectedXp) {
        assertEquals(expectedXp, XpMath.xpNeededForNextLevel(level));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 15, 16, 17, 30, 31, 32, 50, 100, 10_000})
    void stateRoundTripIsExactAtRepresentativeTotals(int level) {
        long totalXp = XpMath.totalXpAtLevel(level);
        XpState state = XpMath.stateFromTotalXp(totalXp);

        assertEquals(totalXp, state.toTotalXp());
        assertEquals(level, state.level());
        assertEquals(0, state.xpIntoLevel());
        assertEquals(0.0, state.normalizedProgress());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 6, 7, 351, 352, 393, 394, 1394, 1395, 30969, 30970, Long.MAX_VALUE})
    void rawXpStateConservesEveryRawPoint(long totalXp) {
        XpState state = XpMath.stateFromTotalXp(totalXp);

        assertEquals(totalXp, state.toTotalXp());
        assertEquals(totalXp, XpMath.totalXpAtLevel(state.level()) + state.xpIntoLevel());
        assertTrue(state.normalizedProgress() >= 0.0 && state.normalizedProgress() < 1.0);
    }

    @org.junit.jupiter.api.Test
    void exhaustiveLevelBoundariesAreExactAndMonotonic() {
        long previousStart = -1;
        for (int level = 0; level <= 10_000; level++) {
            long start = XpMath.totalXpAtLevel(level);
            long nextStart = XpMath.totalXpAtLevel(level + 1);

            assertTrue(start > previousStart, "level starts must be strictly monotonic");
            assertEquals(level, XpMath.levelFromTotalXp(start));
            assertEquals(0, XpMath.xpIntoLevel(start));
            assertEquals(XpMath.xpNeededForNextLevel(level), nextStart - start);

            if (start > 0) {
                assertEquals(level - 1, XpMath.levelFromTotalXp(start - 1));
            }
            assertEquals(level, XpMath.levelFromTotalXp(start + 1));
            assertEquals(1, XpMath.xpIntoLevel(start + 1));
            assertEquals(level, XpMath.levelFromTotalXp(nextStart - 1));
            assertEquals(nextStart - 1 - start, XpMath.xpIntoLevel(nextStart - 1));
            assertEquals(level + 1, XpMath.levelFromTotalXp(nextStart));

            previousStart = start;
        }
    }

    @org.junit.jupiter.api.Test
    void sampledRawXpValuesRoundTripWithoutRoundingLoss() {
        LongStream.iterate(0, value -> value <= 1_000_000, value -> value + 97)
                .forEach(totalXp -> {
                    XpState state = XpMath.stateFromTotalXp(totalXp);
                    assertEquals(totalXp, state.toTotalXp());
                    assertEquals(totalXp, XpMath.totalXpAtLevel(state.level()) + state.xpIntoLevel());
                });
    }

    @org.junit.jupiter.api.Test
    void vanillaFloatProgressReconstructsEveryRepresentativeRawPointExactly() {
        for (int level = 0; level <= 200; level++) {
            int needed = XpMath.xpNeededForNextLevel(level);
            for (int intoLevel : new int[] {0, 1, needed / 2, needed - 1}) {
                long rawXp = XpMath.totalXpAtLevel(level) + intoLevel;
                float progress = (float) intoLevel / needed;
                assertEquals(rawXp, XpMath.totalXpFromLevelProgress(level, progress),
                        "level=" + level + ", intoLevel=" + intoLevel);
            }
            long nextLevel = XpMath.totalXpAtLevel(level + 1);
            assertEquals(nextLevel, XpMath.totalXpFromLevelProgress(level + 1, 0.0F),
                    "next-level boundary after level=" + level);
        }
    }

    @org.junit.jupiter.api.Test
    void reconstructsOwnerCommandDrivenDivergentStatesFromVisibleXp() {
        assertEquals(1_395L, XpMath.totalXpFromLevelProgress(30, 0.0F));
        assertEquals(1_443L, XpMath.totalXpFromLevelProgress(30, 0.42857128F));
        assertEquals(24_045L, XpMath.totalXpFromLevelProgress(90, 0.0F));
        assertEquals(91L, XpMath.totalXpFromLevelProgress(7, 0.0F));
    }

    @org.junit.jupiter.api.Test
    void negativeInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> XpMath.xpNeededForNextLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> XpMath.totalXpAtLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> XpMath.levelFromTotalXp(-1));
        assertThrows(IllegalArgumentException.class, () -> XpMath.xpIntoLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> XpMath.stateFromTotalXp(-1));
        assertThrows(IllegalArgumentException.class, () -> XpMath.totalXpFromLevelProgress(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> XpMath.totalXpFromLevelProgress(0, 1));
    }

    @org.junit.jupiter.api.Test
    void overflowingPublicReturnTypesFailExplicitly() {
        assertThrows(ArithmeticException.class, () -> XpMath.xpNeededForNextLevel(Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> XpMath.totalXpAtLevel(Integer.MAX_VALUE));
    }
}

package com.mentality.xpreserve.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class FlaskFillStateTest {
    @Test
    void allTiersUseTheSameFiveExactFillRanges() {
        for (FlaskTier tier : FlaskTier.values()) {
            long capacity = tier.capacity();
            long lowLast = capacity * 25 / 100;
            long mediumLast = capacity * 50 / 100;
            assertEquals(FlaskFillState.EMPTY, FlaskFillState.from(0, capacity));
            assertEquals(FlaskFillState.LOW, FlaskFillState.from(1, capacity));
            assertEquals(FlaskFillState.LOW, FlaskFillState.from(lowLast, capacity));
            assertEquals(FlaskFillState.MEDIUM, FlaskFillState.from(lowLast + 1, capacity));
            assertEquals(FlaskFillState.MEDIUM, FlaskFillState.from(mediumLast, capacity));
            assertEquals(FlaskFillState.HIGH, FlaskFillState.from(mediumLast + 1, capacity));
            assertEquals(FlaskFillState.HIGH, FlaskFillState.from(capacity - 1, capacity));
            assertEquals(FlaskFillState.FULL, FlaskFillState.from(capacity, capacity));
        }
    }

    @Test
    void displayPercentageIsRoundedAndNeverClaimsPartialIsFull() {
        assertEquals(0, FlaskFillState.percentage(0, FlaskTier.I.capacity()));
        assertEquals(100, FlaskFillState.percentage(FlaskTier.I.capacity(), FlaskTier.I.capacity()));
        assertEquals(99, FlaskFillState.percentage(FlaskTier.I.capacity() - 1, FlaskTier.I.capacity()));
        assertEquals(37, FlaskFillState.percentage(2_000, FlaskTier.II.capacity()));
    }

    @Test
    void invalidVisualInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> FlaskFillState.from(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> FlaskFillState.from(2, 1));
        assertThrows(IllegalArgumentException.class, () -> FlaskFillState.from(0, 0));
    }

    @Test
    void tooltipTranslationKeysExistInBothLocales() throws IOException {
        for (String locale : new String[] {"en_us", "ru_ru"}) {
            String text = new String(getClass().getClassLoader().getResourceAsStream("assets/xp-reserve/lang/" + locale + ".json")
                    .readAllBytes(), StandardCharsets.UTF_8);
            for (String key : new String[] {"stored", "capacity", "percent", "empty", "full", "store", "withdraw"}) {
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("tooltip.xp-reserve.flask." + key));
            }
        }
    }
}

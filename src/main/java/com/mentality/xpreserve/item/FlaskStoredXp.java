package com.mentality.xpreserve.item;

import com.mentality.xpreserve.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** Single validation boundary for Flask I's persistent stored-XP component. */
public final class FlaskStoredXp {
    private FlaskStoredXp() {
    }

    /** Returns zero for an absent component and rejects malformed persisted values. */
    public static long get(ItemStack stack) {
        long storedXp = stack.getOrDefault(ModDataComponents.STORED_XP, 0L);
        validate(storedXp);
        return storedXp;
    }

    /** Writes a valid raw XP amount to this individual flask stack. */
    public static void set(ItemStack stack, long storedXp) {
        validate(storedXp);
        stack.set(ModDataComponents.STORED_XP, storedXp);
    }

    private static void validate(long storedXp) {
        if (storedXp < 0 || storedXp > FlaskItem.CAPACITY) {
            throw new IllegalStateException(
                    "stored_xp must be between 0 and " + FlaskItem.CAPACITY + ", got " + storedXp);
        }
    }
}

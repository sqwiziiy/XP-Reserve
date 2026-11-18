package com.mentality.xpreserve.item;

import com.mentality.xpreserve.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** Single validation boundary for all tier-specific stored-XP component access. */
public final class FlaskStoredXp {
    private FlaskStoredXp() {
    }

    /** Returns zero for an absent component and rejects malformed persisted values. */
    public static long get(ItemStack stack) {
        long storedXp = stack.getOrDefault(ModDataComponents.STORED_XP, 0L);
        validate(stack, storedXp);
        return storedXp;
    }

    /** Writes a valid raw XP amount to this individual flask stack. */
    public static void set(ItemStack stack, long storedXp) {
        validate(stack, storedXp);
        stack.set(ModDataComponents.STORED_XP, storedXp);
        FlaskVisuals.refresh(stack);
    }

    private static void validate(ItemStack stack, long storedXp) {
        if (!(stack.getItem() instanceof FlaskItem flask)) {
            throw new IllegalStateException("stored_xp can only be read from an XP Reserve flask");
        }
        if (storedXp < 0 || storedXp > flask.capacity()) {
            throw new IllegalStateException(
                    "stored_xp must be between 0 and " + flask.capacity() + " for tier " + flask.tier() + ", got " + storedXp);
        }
    }
}

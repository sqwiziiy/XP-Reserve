package com.mentality.xpreserve.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Maintains the non-authoritative model-data mirror derived from stored raw XP. */
public final class FlaskVisuals {
    private FlaskVisuals() { }

    public static void refresh(ItemStack stack) {
        long storedXp = FlaskStoredXp.get(stack);
        FlaskItem flask = flask(stack);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(FlaskFillState.fraction(storedXp, flask.capacity())), List.of(), List.of(), List.of()));
    }

    public static float modelFraction(ItemStack stack) {
        FlaskItem flask = flask(stack);
        return FlaskFillState.fraction(FlaskStoredXp.get(stack), flask.capacity());
    }

    public static FlaskFillState fillState(ItemStack stack) {
        FlaskItem flask = flask(stack);
        return FlaskFillState.from(FlaskStoredXp.get(stack), flask.capacity());
    }

    private static FlaskItem flask(ItemStack stack) {
        if (stack.getItem() instanceof FlaskItem flask) return flask;
        throw new IllegalArgumentException("Visual state requires an XP Reserve flask");
    }
}

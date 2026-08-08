package com.mentality.xpreserve.component;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.item.FlaskItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Registers persistent per-stack state owned by XP Reserve. */
public final class ModDataComponents {
    public static final DataComponentType<Long> STORED_XP = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "stored_xp"),
            DataComponentType.<Long>builder()
                    .persistent(ExtraCodecs.longRange(0, Math.toIntExact(FlaskItem.CAPACITY)))
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build()
    );

    private ModDataComponents() {
    }

    /** Forces class initialization during common mod bootstrap. */
    public static void initialize() {
    }
}

package com.mentality.xpreserve.registry;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.component.ModDataComponents;
import com.mentality.xpreserve.item.FlaskItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Registers XP Reserve items. */
public final class ModItems {
    private static final Identifier FLASK_I_ID = Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "flask_i");

    public static final Item FLASK_I = Registry.register(
            BuiltInRegistries.ITEM,
            FLASK_I_ID,
            new FlaskItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, FLASK_I_ID))
                    .stacksTo(1)
                    .component(ModDataComponents.STORED_XP, 0L))
    );

    private ModItems() {
    }

    /** Forces class initialization during common mod bootstrap. */
    public static void initialize() {
    }
}

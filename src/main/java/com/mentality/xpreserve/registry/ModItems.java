package com.mentality.xpreserve.registry;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.component.ModDataComponents;
import com.mentality.xpreserve.item.FlaskItem;
import com.mentality.xpreserve.item.FlaskTier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import java.util.List;

/** Registers XP Reserve items. */
public final class ModItems {
    private static final Identifier FLASK_I_ID = Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "flask_i");
    private static final Identifier FLASK_II_ID = Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "flask_ii");
    private static final Identifier FLASK_III_ID = Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "flask_iii");
    private static final Identifier SOUL_FLASK_ID = Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "soul_flask");

    public static final Item FLASK_I = Registry.register(
            BuiltInRegistries.ITEM,
            FLASK_I_ID,
            new FlaskItem(FlaskTier.I, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, FLASK_I_ID))
                    .stacksTo(1)
                    .component(ModDataComponents.STORED_XP, 0L)
                    .component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(0.0F), List.of(), List.of(), List.of())))
    );

    public static final Item FLASK_II = registerFlask(FLASK_II_ID, FlaskTier.II);
    public static final Item FLASK_III = registerFlask(FLASK_III_ID, FlaskTier.III);
    public static final Item SOUL_FLASK = registerFlask(SOUL_FLASK_ID, FlaskTier.III);

    private static Item registerFlask(Identifier id, FlaskTier tier) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new FlaskItem(tier, new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
                        .stacksTo(1)
                        .component(ModDataComponents.STORED_XP, 0L)
                        .component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(0.0F), List.of(), List.of(), List.of())))
        );
    }

    private ModItems() {
    }

    /** Forces class initialization during common mod bootstrap. */
    public static void initialize() {
    }
}

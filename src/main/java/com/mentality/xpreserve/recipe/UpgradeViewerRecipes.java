package com.mentality.xpreserve.recipe;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.List;

/** Immutable display-only definitions for optional recipe viewers. */
public final class UpgradeViewerRecipes {
    public record Descriptor(Identifier id, Item source, Item material, Item target) { }
    private static final List<Descriptor> RECIPES = List.of(
            new Descriptor(id("flask_i_upgrade"), ModItems.FLASK_I, Items.DIAMOND_BLOCK, ModItems.FLASK_II),
            new Descriptor(id("flask_ii_upgrade"), ModItems.FLASK_II, Items.RECOVERY_COMPASS, ModItems.FLASK_III),
            new Descriptor(id("soul_flask_upgrade"), ModItems.FLASK_III, Items.TOTEM_OF_UNDYING, ModItems.SOUL_FLASK));
    private UpgradeViewerRecipes() { }
    public static List<Descriptor> all() { return RECIPES; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, path); }
}

package com.mentality.xpreserve.recipe;

import com.mojang.serialization.MapCodec;
import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.registry.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Registers fixed, component-safe Flask upgrade recipe serializers. */
public final class ModRecipeSerializers {
    public static final RecipeSerializer<FlaskUpgradeRecipe> FLASK_I_UPGRADE = register(
            "flask_i_upgrade", ModItems.FLASK_I, Items.DIAMOND_BLOCK, ModItems.FLASK_II);
    public static final RecipeSerializer<FlaskUpgradeRecipe> FLASK_II_UPGRADE = register(
            "flask_ii_upgrade", ModItems.FLASK_II, Items.RECOVERY_COMPASS, ModItems.FLASK_III);
    public static final RecipeSerializer<FlaskUpgradeRecipe> SOUL_FLASK_UPGRADE = register(
            "soul_flask_upgrade", ModItems.FLASK_III, Items.TOTEM_OF_UNDYING, ModItems.SOUL_FLASK);

    private ModRecipeSerializers() { }

    private static RecipeSerializer<FlaskUpgradeRecipe> register(String path, net.minecraft.world.item.Item source,
            net.minecraft.world.item.Item material, net.minecraft.world.item.Item target) {
        final FlaskUpgradeRecipe[] recipe = new FlaskUpgradeRecipe[1];
        RecipeSerializer<FlaskUpgradeRecipe> serializer = new RecipeSerializer<>() {
            @Override public MapCodec<FlaskUpgradeRecipe> codec() { return MapCodec.unit(recipe[0]); }
            @Override public StreamCodec<RegistryFriendlyByteBuf, FlaskUpgradeRecipe> streamCodec() { return StreamCodec.unit(recipe[0]); }
        };
        recipe[0] = new FlaskUpgradeRecipe(source, material, target);
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, path), serializer);
    }

    static RecipeSerializer<FlaskUpgradeRecipe> forSource(net.minecraft.world.item.Item source) {
        if (source == ModItems.FLASK_I) return FLASK_I_UPGRADE;
        if (source == ModItems.FLASK_II) return FLASK_II_UPGRADE;
        if (source == ModItems.FLASK_III) return SOUL_FLASK_UPGRADE;
        throw new IllegalArgumentException("Unknown Flask upgrade source item");
    }

    public static void initialize() { }
}

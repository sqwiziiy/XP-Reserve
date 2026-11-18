package com.mentality.xpreserve.compat.rei;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.recipe.UpgradeViewerRecipes;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultShapelessDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import java.util.List;

public final class XpReserveReiClientPlugin implements REIClientPlugin {
    @Override public void registerDisplays(DisplayRegistry registry) {
        UpgradeViewerRecipes.all().forEach(d -> {
            ShapelessRecipe recipe = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(d.target()),
                    List.of(Ingredient.of(d.source()), Ingredient.of(d.material())));
            RecipeHolder<ShapelessRecipe> holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "rei/" + d.id().getPath())), recipe);
            registry.add(new DefaultShapelessDisplay(holder));
        });
    }
}

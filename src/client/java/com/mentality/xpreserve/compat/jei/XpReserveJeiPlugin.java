package com.mentality.xpreserve.compat.jei;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.recipe.UpgradeViewerRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import java.util.List;

@JeiPlugin
public final class XpReserveJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "jei_plugin"); }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<CraftingRecipe>> recipes = UpgradeViewerRecipes.all().stream().map(d ->
                new RecipeHolder<CraftingRecipe>(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(XpReserveMod.MOD_ID, "jei/" + d.id().getPath())),
                        new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(d.target()),
                                List.of(Ingredient.of(d.source()), Ingredient.of(d.material()))))).toList();
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }
}

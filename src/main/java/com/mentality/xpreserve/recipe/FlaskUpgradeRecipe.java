package com.mentality.xpreserve.recipe;

import com.mentality.xpreserve.item.FlaskStoredXp;
import com.mentality.xpreserve.item.FlaskVisuals;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * A standard shapeless crafting recipe whose functional result preserves only a
 * valid source flask's component state. Extending the vanilla recipe supplies
 * the complete recipe-book and viewer contract (ingredients, result, placement
 * and display) without using that representative result for actual crafting.
 */
public final class FlaskUpgradeRecipe extends ShapelessRecipe {
    private final Item source;
    private final Item material;
    private final Item target;

    public FlaskUpgradeRecipe(Item source, Item material, Item target) {
        super("", CraftingBookCategory.MISC, new ItemStack(target),
                List.of(Ingredient.of(source), Ingredient.of(material)));
        this.source = source;
        this.material = material;
        this.target = target;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }
        ItemStack sourceStack = ItemStack.EMPTY;
        boolean materialFound = false;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(source) && sourceStack.isEmpty()) sourceStack = stack;
            else if (stack.is(material) && !materialFound) materialFound = true;
            else return false;
        }
        if (sourceStack.isEmpty() || !materialFound) return false;
        try {
            FlaskStoredXp.get(sourceStack);
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (stack.is(source)) {
                try {
                    FlaskStoredXp.get(stack);
                    ItemStack result = stack.transmuteCopy(target, 1);
                    FlaskStoredXp.get(result);
                    FlaskVisuals.refresh(result);
                    return result;
                } catch (IllegalStateException ignored) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override public RecipeSerializer<ShapelessRecipe> getSerializer() { return ModRecipeSerializers.forSource(source); }
}

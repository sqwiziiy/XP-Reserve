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
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import java.util.List;

/** Two-item upgrade recipe that preserves only valid source-flask component state. */
public final class FlaskUpgradeRecipe implements CraftingRecipe {
    private final Item source;
    private final Item material;
    private final Item target;

    public FlaskUpgradeRecipe(Item source, Item material, Item target) {
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

    @Override public RecipeSerializer<FlaskUpgradeRecipe> getSerializer() { return ModRecipeSerializers.forSource(source); }
    @Override public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(java.util.List.of(Ingredient.of(source), Ingredient.of(material))); }

    /**
     * Custom recipes do not inherit vanilla's recipe-book display. Supply the two
     * ingredients and a fresh target flask explicitly while retaining the strict
     * runtime component validation in {@link #matches(CraftingInput, Level)}.
     */
    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(new SlotDisplay.ItemSlotDisplay(source), new SlotDisplay.ItemSlotDisplay(material)),
                new SlotDisplay.ItemStackSlotDisplay(new ItemStack(target)),
                new SlotDisplay.ItemSlotDisplay(net.minecraft.world.item.Items.CRAFTING_TABLE)));
    }
}

package com.mentality.xpreserve.gametest;

import com.mentality.xpreserve.item.FlaskItem;
import com.mentality.xpreserve.item.FlaskStoredXp;
import com.mentality.xpreserve.item.FlaskTier;
import com.mentality.xpreserve.registry.ModItems;
import com.mentality.xpreserve.xp.PlayerXp;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

/** Runtime acceptance tests for the production Flask I registration and interaction path. */
public final class FlaskRuntimeGameTests {
    @GameTest
    public void itemStackComponentLifecycle(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModItems.FLASK_I);
        helper.assertTrue(fresh.is(ModItems.FLASK_I), "Fresh stack must use the production Flask I item");
        helper.assertValueEqual(1, fresh.getCount(), "Fresh Flask I count must be one");
        helper.assertValueEqual(1, fresh.getMaxStackSize(), "Flask I max stack size must be one");
        helper.assertValueEqual(0L, FlaskStoredXp.get(fresh), "Fresh Flask I stored XP must be zero");

        for (long storedXp : new long[] {0, 1, 394, 1_000, FlaskTier.I.capacity()}) {
            FlaskStoredXp.set(fresh, storedXp);
            helper.assertValueEqual(storedXp, FlaskStoredXp.get(fresh), "Stored XP must round-trip through the component");

            ItemStack copied = fresh.copy();
            helper.assertTrue(copied.is(ModItems.FLASK_I), "Copied stack must retain Flask I item identity");
            helper.assertValueEqual(1, copied.getCount(), "Copied Flask I count must remain one");
            helper.assertValueEqual(storedXp, FlaskStoredXp.get(copied), "Copied stack must retain stored XP");

            FlaskStoredXp.set(copied, 0L);
            helper.assertValueEqual(storedXp, FlaskStoredXp.get(fresh), "Mutating a copy must not mutate its original");

            RegistryOps<Tag> registryOps = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack decoded = ItemStack.CODEC.parse(registryOps, ItemStack.CODEC.encodeStart(registryOps, fresh).getOrThrow())
                    .getOrThrow();
            helper.assertTrue(decoded.is(ModItems.FLASK_I), "Serialized stack must retain Flask I item identity");
            helper.assertValueEqual(1, decoded.getCount(), "Serialized Flask I count must remain one");
            helper.assertValueEqual(storedXp, FlaskStoredXp.get(decoded), "Serialized stack must retain exact stored XP");
        }

        assertInvalidValueRejected(helper, fresh, -1L);
        assertInvalidValueRejected(helper, fresh, FlaskTier.I.capacity() + 1);
        helper.succeed();
    }

    @GameTest
    public void serverSideFlaskInteractionsConserveXp(GameTestHelper helper) {
        assertDeposit(helper, 1_000, 0, 0, 1_000);
        assertDeposit(helper, ModItems.FLASK_I, 2_000, 0, 605, FlaskTier.I.capacity());
        assertDeposit(helper, ModItems.FLASK_I, 1_000, 1_000, 605, FlaskTier.I.capacity());
        assertDeposit(helper, ModItems.FLASK_I, 500, FlaskTier.I.capacity(), 500, FlaskTier.I.capacity());
        assertWithdrawal(helper, 1_000, 395, 1_395, 0);
        assertWithdrawal(helper, 500, 0, 500, 0);
        assertRepeatedCycles(helper);
        helper.succeed();
    }

    @GameTest
    public void higherTierInteractionsAndSerialization(GameTestHelper helper) {
        assertDeposit(helper, ModItems.FLASK_II, 7_000, 0, 1_655, FlaskTier.II.capacity());
        assertWithdrawal(helper, ModItems.FLASK_II, 1_655, FlaskTier.II.capacity(), 7_000, 0);
        assertDeposit(helper, ModItems.FLASK_III, 40_000, 0, 9_030, FlaskTier.III.capacity());
        assertWithdrawal(helper, ModItems.FLASK_III, 9_030, FlaskTier.III.capacity(), 40_000, 0);
        assertSerialized(helper, ModItems.FLASK_II, FlaskTier.II.capacity());
        assertSerialized(helper, ModItems.FLASK_III, FlaskTier.III.capacity());
        helper.succeed();
    }

    @GameTest
    public void loadedProgressionRecipesPreserveStoredXp(GameTestHelper helper) {
        CraftingRecipe flaskIRecipe = loadedCraftingRecipe(helper, "flask_i");
        CraftingInput flaskIInput = CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.LAPIS_LAZULI),
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.AMETHYST_SHARD)));
        helper.assertTrue(flaskIRecipe.matches(flaskIInput, helper.getLevel()), "Flask I shaped recipe must match its documented layout");
        ItemStack flaskI = flaskIRecipe.assemble(flaskIInput, helper.getLevel().registryAccess());
        helper.assertTrue(flaskI.is(ModItems.FLASK_I), "Flask I recipe must produce Flask I");
        helper.assertValueEqual(1, flaskI.getCount(), "Flask I recipe must produce exactly one item");
        helper.assertValueEqual(0L, FlaskStoredXp.get(flaskI), "Crafted Flask I must be empty");

        assertUpgrade(helper, "flask_i_upgrade", ModItems.FLASK_I, Items.DIAMOND_BLOCK, ModItems.FLASK_II, 0);
        assertUpgrade(helper, "flask_i_upgrade", ModItems.FLASK_I, Items.DIAMOND_BLOCK, ModItems.FLASK_II, FlaskTier.I.capacity());
        assertUpgrade(helper, "flask_ii_upgrade", ModItems.FLASK_II, Items.RECOVERY_COMPASS, ModItems.FLASK_III, 0);
        assertUpgrade(helper, "flask_ii_upgrade", ModItems.FLASK_II, Items.RECOVERY_COMPASS, ModItems.FLASK_III, FlaskTier.II.capacity());
        helper.succeed();
    }

    private static void assertUpgrade(GameTestHelper helper, String recipeId, net.minecraft.world.item.Item sourceItem,
            net.minecraft.world.item.Item material, net.minecraft.world.item.Item targetItem, long storedXp) {
        CraftingRecipe recipe = loadedCraftingRecipe(helper, recipeId);
        ItemStack source = new ItemStack(sourceItem);
        FlaskStoredXp.set(source, storedXp);
        CraftingInput input = CraftingInput.of(2, 1, java.util.List.of(source, new ItemStack(material)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), recipeId + " must match its source and material");
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(targetItem), recipeId + " must produce its target flask tier");
        helper.assertValueEqual(storedXp, FlaskStoredXp.get(result), recipeId + " must preserve stored XP exactly");
    }

    private static CraftingRecipe loadedCraftingRecipe(GameTestHelper helper, String path) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("xp-reserve", path));
        Recipe<?> recipe = helper.getLevel().getServer().getRecipeManager().byKey(key)
                .orElseThrow(() -> new IllegalStateException("Missing loaded recipe: " + key.identifier())).value();
        if (recipe instanceof CraftingRecipe craftingRecipe) return craftingRecipe;
        throw new IllegalStateException("Loaded recipe is not a crafting recipe: " + key.identifier());
    }

    private static void assertDeposit(
            GameTestHelper helper, long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        assertDeposit(helper, ModItems.FLASK_I, playerXp, storedXp, expectedPlayerXp, expectedStoredXp);
    }

    private static void assertDeposit(GameTestHelper helper, net.minecraft.world.item.Item item,
            long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        ServerPlayer player = playerWithFlask(helper, item, playerXp, storedXp);
        ItemStack flask = player.getItemInHand(InteractionHand.MAIN_HAND);
        long beforeTotal = PlayerXp.currentRawXp(player) + FlaskStoredXp.get(flask);

        player.setShiftKeyDown(true);
        ((FlaskItem) flask.getItem()).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertState(helper, player, flask, expectedPlayerXp, expectedStoredXp, beforeTotal, "deposit");
    }

    private static void assertWithdrawal(
            GameTestHelper helper, long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        assertWithdrawal(helper, ModItems.FLASK_I, playerXp, storedXp, expectedPlayerXp, expectedStoredXp);
    }

    private static void assertWithdrawal(GameTestHelper helper, net.minecraft.world.item.Item item,
            long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        ServerPlayer player = playerWithFlask(helper, item, playerXp, storedXp);
        ItemStack flask = player.getItemInHand(InteractionHand.MAIN_HAND);
        long beforeTotal = PlayerXp.currentRawXp(player) + FlaskStoredXp.get(flask);

        player.setShiftKeyDown(false);
        ((FlaskItem) flask.getItem()).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertState(helper, player, flask, expectedPlayerXp, expectedStoredXp, beforeTotal, "withdrawal");
    }

    private static void assertRepeatedCycles(GameTestHelper helper) {
        ServerPlayer player = playerWithFlask(helper, ModItems.FLASK_I, 1_000, 0);
        ItemStack flask = player.getItemInHand(InteractionHand.MAIN_HAND);
        long initialTotal = PlayerXp.currentRawXp(player) + FlaskStoredXp.get(flask);

        for (int cycle = 0; cycle < 100; cycle++) {
            player.setShiftKeyDown(true);
            ((FlaskItem) ModItems.FLASK_I).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            ((FlaskItem) ModItems.FLASK_I).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }

        assertState(helper, player, flask, 1_000, 0, initialTotal, "100 deposit/withdraw cycles");
    }

    private static void assertSerialized(GameTestHelper helper, net.minecraft.world.item.Item item, long storedXp) {
        ItemStack stack = new ItemStack(item);
        FlaskStoredXp.set(stack, storedXp);
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack decoded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.is(item), "Serialized tier stack must retain item identity");
        helper.assertValueEqual(storedXp, FlaskStoredXp.get(decoded), "Serialized tier stack must retain stored XP");
    }

    private static ServerPlayer playerWithFlask(GameTestHelper helper, net.minecraft.world.item.Item item, long playerXp, long storedXp) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.giveExperiencePoints(Math.toIntExact(playerXp));
        ItemStack flask = new ItemStack(item);
        FlaskStoredXp.set(flask, storedXp);
        player.setItemInHand(InteractionHand.MAIN_HAND, flask);
        helper.assertValueEqual(playerXp, PlayerXp.currentRawXp(player), "Player XP setup must be exact");
        return player;
    }

    private static void assertState(
            GameTestHelper helper,
            ServerPlayer player,
            ItemStack flask,
            long expectedPlayerXp,
            long expectedStoredXp,
            long expectedTotal,
            String scenario) {
        long actualPlayerXp = PlayerXp.currentRawXp(player);
        long actualStoredXp = FlaskStoredXp.get(flask);
        helper.assertValueEqual(expectedPlayerXp, actualPlayerXp, scenario + " player XP must match");
        helper.assertValueEqual(expectedStoredXp, actualStoredXp, scenario + " stored XP must match");
        helper.assertValueEqual(expectedTotal, actualPlayerXp + actualStoredXp, scenario + " must conserve raw XP");
    }

    private static void assertInvalidValueRejected(GameTestHelper helper, ItemStack flask, long value) {
        try {
            FlaskStoredXp.set(flask, value);
            helper.fail("Invalid stored XP value was accepted: " + value);
        } catch (IllegalStateException expected) {
            // Production policy rejects invalid stored values.
        }
    }
}

package com.mentality.xpreserve.gametest;

import com.mentality.xpreserve.item.FlaskItem;
import com.mentality.xpreserve.item.FlaskStoredXp;
import com.mentality.xpreserve.item.FlaskTier;
import com.mentality.xpreserve.item.FlaskFillState;
import com.mentality.xpreserve.item.FlaskVisuals;
import com.mentality.xpreserve.registry.ModItems;
import com.mentality.xpreserve.xp.PlayerXp;
import com.mentality.xpreserve.xp.XpMath;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;

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

    @GameTest
    public void fullOnlyGlintAndVisualMirrorsAreDerivedFromStoredXp(GameTestHelper helper) {
        for (net.minecraft.world.item.Item item : java.util.List.of(ModItems.FLASK_I, ModItems.FLASK_II, ModItems.FLASK_III, ModItems.SOUL_FLASK)) {
            FlaskItem flaskItem = (FlaskItem) item;
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(!flaskItem.isFoil(stack), "Empty flask must not glint");
            helper.assertValueEqual(FlaskFillState.EMPTY, FlaskVisuals.fillState(stack), "Empty flask must be visually empty");
            FlaskStoredXp.set(stack, 1);
            helper.assertTrue(!flaskItem.isFoil(stack), "Partial flask must not glint");
            helper.assertValueEqual(FlaskFillState.LOW, FlaskVisuals.fillState(stack), "One XP must be low fill");
            FlaskStoredXp.set(stack, flaskItem.capacity());
            helper.assertTrue(flaskItem.isFoil(stack), "Full flask must glint");
            helper.assertValueEqual(FlaskFillState.FULL, FlaskVisuals.fillState(stack), "Full flask must be visually full");
        }
        helper.succeed();
    }

    @GameTest
    public void soulFlaskRecipeAndSerializationPreserveStoredXp(GameTestHelper helper) {
        for (long storedXp : new long[] {0, 1, 12_345, FlaskTier.III.capacity()}) {
            assertUpgrade(helper, "soul_flask_upgrade", ModItems.FLASK_III, Items.TOTEM_OF_UNDYING, ModItems.SOUL_FLASK, storedXp);
            assertSerialized(helper, ModItems.SOUL_FLASK, storedXp);
        }
        helper.succeed();
    }

    @GameTest
    public void soulFlaskCapturesOnRealDeathAndDropsConvertedItem(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 1_000, 0);
        helper.assertFalse(player.getAbilities().instabuild, "Death test player must not have creative inventory protection");
        player.die(helper.getLevel().damageSources().genericKill());
        ItemStack dropped = onlyDropped(helper, ModItems.FLASK_III);
        helper.assertValueEqual(1_000L, FlaskStoredXp.get(dropped), "Real death must drop one Flask III with captured XP");
        assertVanillaXpState(helper, player, 0, "Full death capture");
        helper.assertValueEqual(0L, experienceOrbValue(helper), "Full death capture must create no vanilla death XP orbs");
        helper.assertValueEqual(0L, helper.getEntities(EntityType.ITEM).stream().filter(item -> item.getItem().is(ModItems.SOUL_FLASK)).count(),
                "Real death must not leave the armed Soul Flask in drops");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskCapacityLimitLeavesVanillaDeathXpBasis(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 5_000, 30_000);
        player.die(helper.getLevel().damageSources().genericKill());
        ItemStack dropped = onlyDropped(helper, ModItems.FLASK_III);
        helper.assertValueEqual(30_970L, FlaskStoredXp.get(dropped), "Capacity-limited death capture must fill only free capacity");
        helper.assertValueEqual(4_030L, PlayerXp.currentRawXp(player), "Vanilla death must receive the post-capture 4030 XP basis");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskLowRemainderUsesPostCaptureVanillaReward(GameTestHelper helper) {
        setKeepInventory(helper, false);
        // 1000 raw XP is level 26, so its vanilla reward is capped at 100. Filling all but
        // 901 Soul capacity leaves 99 raw XP (level 7), whose vanilla reward is 49.
        ServerPlayer insured = playerWithFlask(helper, ModItems.SOUL_FLASK, 1_000, 30_069);
        insured.die(helper.getLevel().damageSources().genericKill());
        long insuredReward = experienceOrbValue(helper);
        helper.assertValueEqual(49L, insuredReward, "Low post-capture remainder must use its uncapped 49-XP vanilla reward");
        helper.assertValueEqual(30_970L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)), "Soul Flask must capture the 901 free capacity");
        assertVanillaXpState(helper, insured, 99, "Low-remainder capture");

        helper.killAllEntitiesOfClass(ExperienceOrb.class);
        ServerPlayer baseline = helper.makeMockServerPlayerInLevel();
        positionInTest(helper, baseline);
        baseline.getAbilities().instabuild = false;
        baseline.getAbilities().invulnerable = false;
        baseline.giveExperiencePoints(99);
        baseline.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(49L, experienceOrbValue(helper), "Vanilla 99-XP baseline must have the same 49-XP reward");
        helper.assertTrue(insuredReward != 100L, "Post-capture reward must differ from the pre-capture 100-XP reward");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskPartialPrefillAndZeroXpFollowExactDeathContract(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer partial = playerWithFlask(helper, ModItems.SOUL_FLASK, 5_000, 10_000);
        partial.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(15_000L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)),
                "Partial Soul Flask prefill must retain old XP and capture all 5000 player XP");
        assertVanillaXpState(helper, partial, 0, "Partial full capture");
        helper.assertValueEqual(0L, experienceOrbValue(helper), "Partial full capture must create no vanilla death XP orbs");

        ServerPlayer zero = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 0);
        zero.die(helper.getLevel().damageSources().genericKill());
        java.util.List<ItemEntity> souls = helper.getEntities(EntityType.ITEM).stream()
                .filter(entity -> entity.getItem().is(ModItems.SOUL_FLASK)).toList();
        helper.assertValueEqual(1, souls.size(), "Zero XP death must leave Soul Flask armed");
        helper.assertValueEqual(0L, FlaskStoredXp.get(souls.getFirst().getItem()), "Zero XP death must not change stored XP");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskDeathUsesVisibleXpWhenTotalExperienceIsStale(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer levelThirty = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 0);
        setVisibleXp(levelThirty, 100, 30, 0.42857128F);
        helper.assertValueEqual(1_443L, PlayerXp.currentRawXp(levelThirty), "30-level visible XP must not read stale totalExperience=100");
        levelThirty.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(1_443L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)), "Soul death must capture reconstructed 30-level XP");
        helper.assertValueEqual(0L, experienceOrbValue(helper), "Full reconstructed capture must not leak death XP");
        helper.killAllEntitiesOfClass(ItemEntity.class);

        ServerPlayer levelNinety = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 100);
        setVisibleXp(levelNinety, 0, 90, 0.0F);
        helper.assertValueEqual(24_045L, PlayerXp.currentRawXp(levelNinety), "90-level visible XP must not read stale totalExperience=0");
        levelNinety.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(24_145L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)), "Partial Soul Flask must add reconstructed 90-level XP");
        helper.succeed();
    }

    @GameTest
    public void depositsUseVisibleXpWhenTotalExperienceIsStale(GameTestHelper helper) {
        ServerPlayer flaskIPlayer = playerWithFlask(helper, ModItems.FLASK_I, 0, 0);
        setVisibleXp(flaskIPlayer, 100, 30, 0.42857128F);
        flaskIPlayer.setShiftKeyDown(true);
        ((FlaskItem) ModItems.FLASK_I).use(helper.getLevel(), flaskIPlayer, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(FlaskTier.I.capacity(), FlaskStoredXp.get(flaskIPlayer.getMainHandItem()), "Flask I deposit must use visible 1443 XP");
        assertVanillaXpState(helper, flaskIPlayer, 48, "Flask I divergent-state deposit");

        ServerPlayer soulPlayer = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 0);
        setVisibleXp(soulPlayer, 100, 30, 0.42857128F);
        soulPlayer.setShiftKeyDown(true);
        ((FlaskItem) ModItems.SOUL_FLASK).use(helper.getLevel(), soulPlayer, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(1_443L, FlaskStoredXp.get(soulPlayer.getMainHandItem()), "Soul deposit must use reconstructed visible XP");
        assertVanillaXpState(helper, soulPlayer, 0, "Soul divergent-state deposit");

        ServerPlayer levelThirtyZeroProgress = playerWithFlask(helper, ModItems.FLASK_I, 0, 0);
        setVisibleXp(levelThirtyZeroProgress, 0, 30, 0.0F);
        helper.assertValueEqual(1_395L, PlayerXp.currentRawXp(levelThirtyZeroProgress),
                "Visible level 30 must win over totalExperience=0");

        ServerPlayer levelSeven = playerWithFlask(helper, ModItems.FLASK_I, 0, 0);
        setVisibleXp(levelSeven, 999_999, 7, 0.0F);
        helper.assertValueEqual(91L, PlayerXp.currentRawXp(levelSeven),
                "Visible level 7 must win over totalExperience=999999");
        helper.succeed();
    }

    @GameTest
    public void normalReleaseUsesVisibleXpWhenTotalExperienceIsStale(GameTestHelper helper) {
        ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 500);
        setVisibleXp(player, 100, 30, 0.42857128F);

        ((FlaskItem) ModItems.SOUL_FLASK).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertValueEqual(0L, FlaskStoredXp.get(player.getMainHandItem()), "Normal release must empty the Soul Flask");
        assertVanillaXpState(helper, player, 1_943, "Normal release from divergent visible XP");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskDeathLeavesOtherFlaskTiersUntouched(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 4_000, 0);
        player.getInventory().setItem(1, flask(ModItems.FLASK_I, 1_000));
        player.getInventory().setItem(2, flask(ModItems.FLASK_II, 2_000));
        player.getInventory().setItem(3, flask(ModItems.FLASK_III, 3_000));
        player.die(helper.getLevel().damageSources().genericKill());
        long capturedFlaskIii = helper.getEntities(EntityType.ITEM).stream().filter(entity -> entity.getItem().is(ModItems.FLASK_III))
                .mapToLong(entity -> FlaskStoredXp.get(entity.getItem())).filter(value -> value == 4_000L).count();
        helper.assertValueEqual(1L, capturedFlaskIii, "Only player XP must be captured");
        helper.assertValueEqual(1_000L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_I)), "Flask I must be unchanged");
        helper.assertValueEqual(2_000L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_II)), "Flask II must be unchanged");
        long untouchedFlaskIii = helper.getEntities(EntityType.ITEM).stream().filter(entity -> entity.getItem().is(ModItems.FLASK_III))
                .mapToLong(entity -> FlaskStoredXp.get(entity.getItem())).filter(value -> value == 3_000L).count();
        helper.assertValueEqual(1L, untouchedFlaskIii, "Existing Flask III must be unchanged");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskHighXpDeathMatchesVanillaPostCaptureOrbOutput(GameTestHelper helper) {
        setKeepInventory(helper, false);
        ServerPlayer insured = playerWithFlask(helper, ModItems.SOUL_FLASK, 40_000, 0);
        insured.die(helper.getLevel().damageSources().genericKill());
        long insuredOrbs = experienceOrbValue(helper);
        helper.assertValueEqual(100L, insuredOrbs, "Vanilla 9030-XP post-capture death must emit its capped 100-XP reward");
        helper.assertValueEqual(30_970L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)), "High-XP death must capture only Flask III capacity");

        helper.killAllEntitiesOfClass(ExperienceOrb.class);
        ServerPlayer baseline = helper.makeMockServerPlayerInLevel();
        positionInTest(helper, baseline);
        baseline.getAbilities().instabuild = false;
        baseline.getAbilities().invulnerable = false;
        baseline.giveExperiencePoints(9_030);
        baseline.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(insuredOrbs, experienceOrbValue(helper), "Death XP orbs must match vanilla's 9030-XP post-capture baseline");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskTotemAndPriorityRespectRealDeathLifecycle(GameTestHelper helper) {
        ServerPlayer totemPlayer = playerWithFlask(helper, ModItems.SOUL_FLASK, 10_000, 0);
        totemPlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        totemPlayer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), Float.MAX_VALUE);
        helper.assertTrue(totemPlayer.isAlive(), "Real lethal damage with a Totem must keep the player alive");
        helper.assertTrue(totemPlayer.getMainHandItem().is(ModItems.SOUL_FLASK), "Totem prevention must not consume Soul Flask insurance");
        helper.assertValueEqual(0L, FlaskStoredXp.get(totemPlayer.getMainHandItem()), "Totem prevention must not capture XP");

        setKeepInventory(helper, false);
        ServerPlayer priorityPlayer = playerWithFlask(helper, ModItems.SOUL_FLASK, 1_000, FlaskTier.III.capacity());
        ItemStack offhand = soul(0);
        ItemStack hotbar = soul(0);
        priorityPlayer.setItemInHand(InteractionHand.OFF_HAND, offhand);
        priorityPlayer.getInventory().setItem(1, hotbar);
        priorityPlayer.die(helper.getLevel().damageSources().genericKill());
        helper.assertValueEqual(1_000L, FlaskStoredXp.get(onlyDropped(helper, ModItems.FLASK_III)), "Full main hand must be skipped and offhand must win before hotbar");
        java.util.List<ItemEntity> untouchedSoulDrops = helper.getEntities(EntityType.ITEM).stream()
                .filter(entity -> entity.getItem().is(ModItems.SOUL_FLASK)).toList();
        helper.assertValueEqual(2, untouchedSoulDrops.size(), "Full main-hand and lower-priority hotbar Soul Flasks must remain armed in death drops");
        helper.assertValueEqual(1L, untouchedSoulDrops.stream().filter(drop -> FlaskStoredXp.get(drop.getItem()) == 0).count(),
                "Lower-priority Soul Flask must retain its stored XP");
        helper.assertValueEqual(1L, untouchedSoulDrops.stream().filter(drop -> FlaskStoredXp.get(drop.getItem()) == FlaskTier.III.capacity()).count(),
                "Full higher-priority Soul Flask must be skipped rather than consumed");
        helper.succeed();
    }

    @GameTest
    public void soulFlaskDoesNotTriggerWhenKeepInventoryPreservesXp(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            setKeepInventory(helper, true);
            ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 1_000, 0);
            player.die(helper.getLevel().damageSources().genericKill());
            helper.assertTrue(player.getMainHandItem().is(ModItems.SOUL_FLASK), "keepInventory must preserve armed Soul Flask identity");
            helper.assertValueEqual(0L, FlaskStoredXp.get(player.getMainHandItem()), "keepInventory must not capture XP");
            helper.assertValueEqual(1_000L, PlayerXp.currentRawXp(player), "keepInventory must preserve player XP");
            setKeepInventory(helper, false);
            helper.succeed();
        });
    }

    @GameTest
    public void soulFlaskManualReleaseRepairsMendingGearWithoutDisarming(GameTestHelper helper) {
        ServerPlayer player = playerWithFlask(helper, ModItems.SOUL_FLASK, 0, 500);
        ItemStack mendingTool = new ItemStack(Items.IRON_PICKAXE);
        mendingTool.setDamageValue(100);
        mendingTool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.MENDING), 1);
        player.setItemInHand(InteractionHand.OFF_HAND, mendingTool);

        ((FlaskItem) ModItems.SOUL_FLASK).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ModItems.SOUL_FLASK), "Manual release must leave Soul Flask armed");
        helper.assertValueEqual(0L, FlaskStoredXp.get(player.getMainHandItem()), "Manual release must empty Soul Flask normally");
        helper.assertTrue(mendingTool.getDamageValue() < 100, "Vanilla Mending must repair the damaged eligible item");
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
        FlaskItem target = (FlaskItem) targetItem;
        helper.assertValueEqual(FlaskFillState.from(storedXp, target.capacity()), FlaskVisuals.fillState(result),
                recipeId + " must derive visuals for the target tier capacity");
        helper.assertTrue(target.isFoil(result) == (storedXp == target.capacity()),
                recipeId + " glint must be based on target tier fullness");
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

    private static ItemStack soul(long storedXp) {
        ItemStack stack = new ItemStack(ModItems.SOUL_FLASK);
        FlaskStoredXp.set(stack, storedXp);
        return stack;
    }

    private static ItemStack flask(net.minecraft.world.item.Item item, long storedXp) {
        ItemStack stack = new ItemStack(item);
        FlaskStoredXp.set(stack, storedXp);
        return stack;
    }

    private static ItemStack onlyDropped(GameTestHelper helper, net.minecraft.world.item.Item item) {
        java.util.List<ItemEntity> drops = helper.getEntities(EntityType.ITEM).stream().filter(entity -> entity.getItem().is(item)).toList();
        helper.assertValueEqual(1, drops.size(), "Expected exactly one dropped " + item);
        return drops.getFirst().getItem();
    }

    private static long experienceOrbValue(GameTestHelper helper) {
        return helper.getEntities(EntityType.EXPERIENCE_ORB).stream().mapToLong(ExperienceOrb::getValue).sum();
    }

    private static ServerPlayer playerWithFlask(GameTestHelper helper, net.minecraft.world.item.Item item, long playerXp, long storedXp) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        positionInTest(helper, player);
        player.setGameMode(GameType.SURVIVAL);
        player.giveExperiencePoints(Math.toIntExact(playerXp));
        ItemStack flask = new ItemStack(item);
        FlaskStoredXp.set(flask, storedXp);
        player.setItemInHand(InteractionHand.MAIN_HAND, flask);
        helper.assertValueEqual(playerXp, PlayerXp.currentRawXp(player), "Player XP setup must be exact");
        return player;
    }

    private static void positionInTest(GameTestHelper helper, ServerPlayer player) {
        net.minecraft.core.BlockPos position = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
    }

    private static void setVisibleXp(ServerPlayer player, int totalExperienceField, int level, float progress) {
        player.totalExperience = totalExperienceField;
        player.experienceLevel = level;
        player.experienceProgress = progress;
    }

    private static void setKeepInventory(GameTestHelper helper, boolean value) {
        helper.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, value, helper.getLevel().getServer());
        helper.assertValueEqual(value, helper.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY), "keepInventory setup must apply");
    }

    private static void assertVanillaXpState(GameTestHelper helper, ServerPlayer player, long rawXp, String scenario) {
        com.mentality.xpreserve.xp.XpState state = XpMath.stateFromTotalXp(rawXp);
        helper.assertValueEqual(rawXp, PlayerXp.currentRawXp(player), scenario + " total XP must be exact");
        helper.assertValueEqual(state.level(), player.experienceLevel, scenario + " level must match raw XP");
        float expectedProgress = (float) state.xpIntoLevel() / state.xpNeededForNextLevel();
        helper.assertTrue(Math.abs(player.experienceProgress - expectedProgress) < 0.00001F,
                scenario + " progress must match raw XP");
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

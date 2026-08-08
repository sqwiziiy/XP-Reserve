package com.mentality.xpreserve.gametest;

import com.mentality.xpreserve.item.FlaskItem;
import com.mentality.xpreserve.item.FlaskStoredXp;
import com.mentality.xpreserve.registry.ModItems;
import com.mentality.xpreserve.xp.PlayerXp;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Runtime acceptance tests for the production Flask I registration and interaction path. */
public final class FlaskRuntimeGameTests {
    @GameTest
    public void itemStackComponentLifecycle(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModItems.FLASK_I);
        helper.assertTrue(fresh.is(ModItems.FLASK_I), "Fresh stack must use the production Flask I item");
        helper.assertValueEqual(1, fresh.getCount(), "Fresh Flask I count must be one");
        helper.assertValueEqual(1, fresh.getMaxStackSize(), "Flask I max stack size must be one");
        helper.assertValueEqual(0L, FlaskStoredXp.get(fresh), "Fresh Flask I stored XP must be zero");

        for (long storedXp : new long[] {0, 1, 394, 1_000, FlaskItem.CAPACITY}) {
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
        assertInvalidValueRejected(helper, fresh, FlaskItem.CAPACITY + 1);
        helper.succeed();
    }

    @GameTest
    public void serverSideFlaskInteractionsConserveXp(GameTestHelper helper) {
        assertDeposit(helper, 1_000, 0, 0, 1_000);
        assertDeposit(helper, 2_000, 0, 605, FlaskItem.CAPACITY);
        assertDeposit(helper, 1_000, 1_000, 605, FlaskItem.CAPACITY);
        assertDeposit(helper, 500, FlaskItem.CAPACITY, 500, FlaskItem.CAPACITY);
        assertWithdrawal(helper, 1_000, 395, 1_395, 0);
        assertWithdrawal(helper, 500, 0, 500, 0);
        assertRepeatedCycles(helper);
        helper.succeed();
    }

    private static void assertDeposit(
            GameTestHelper helper, long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        ServerPlayer player = playerWithFlask(helper, playerXp, storedXp);
        ItemStack flask = player.getItemInHand(InteractionHand.MAIN_HAND);
        long beforeTotal = PlayerXp.currentRawXp(player) + FlaskStoredXp.get(flask);

        player.setShiftKeyDown(true);
        ((FlaskItem) ModItems.FLASK_I).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertState(helper, player, flask, expectedPlayerXp, expectedStoredXp, beforeTotal, "deposit");
    }

    private static void assertWithdrawal(
            GameTestHelper helper, long playerXp, long storedXp, long expectedPlayerXp, long expectedStoredXp) {
        ServerPlayer player = playerWithFlask(helper, playerXp, storedXp);
        ItemStack flask = player.getItemInHand(InteractionHand.MAIN_HAND);
        long beforeTotal = PlayerXp.currentRawXp(player) + FlaskStoredXp.get(flask);

        player.setShiftKeyDown(false);
        ((FlaskItem) ModItems.FLASK_I).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertState(helper, player, flask, expectedPlayerXp, expectedStoredXp, beforeTotal, "withdrawal");
    }

    private static void assertRepeatedCycles(GameTestHelper helper) {
        ServerPlayer player = playerWithFlask(helper, 1_000, 0);
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

    private static ServerPlayer playerWithFlask(GameTestHelper helper, long playerXp, long storedXp) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.giveExperiencePoints(Math.toIntExact(playerXp));
        ItemStack flask = new ItemStack(ModItems.FLASK_I);
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

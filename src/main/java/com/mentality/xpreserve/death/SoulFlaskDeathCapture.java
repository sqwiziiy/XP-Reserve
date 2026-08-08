package com.mentality.xpreserve.death;

import com.mentality.xpreserve.item.FlaskStoredXp;
import com.mentality.xpreserve.item.FlaskVisuals;
import com.mentality.xpreserve.item.FlaskItem;
import com.mentality.xpreserve.registry.ModItems;
import com.mentality.xpreserve.xp.PlayerXp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Pre-death capture using the Soul Flask stack itself as all persistent insurance state. */
public final class SoulFlaskDeathCapture {
    private SoulFlaskDeathCapture() { }

    public static boolean capture(ServerPlayer player) {
        long playerXp = PlayerXp.currentRawXp(player);
        if (playerXp == 0) return false;
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        if (tryCapture(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), playerXp)) return true;
        if (tryCapture(player, InteractionHand.OFF_HAND, player.getOffhandItem(), playerXp)) return true;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (slot != selectedSlot && tryCapture(player, slot, inventory.getItem(slot), playerXp)) return true;
        }
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.SLOT_OFFHAND; slot++) {
            if (tryCapture(player, slot, inventory.getItem(slot), playerXp)) return true;
        }
        return false;
    }

    private static boolean tryCapture(ServerPlayer player, InteractionHand hand, ItemStack soul, long playerXp) {
        return tryCapture(player, hand, -1, soul, playerXp);
    }

    private static boolean tryCapture(ServerPlayer player, int slot, ItemStack soul, long playerXp) {
        return tryCapture(player, null, slot, soul, playerXp);
    }

    private static boolean tryCapture(ServerPlayer player, InteractionHand hand, int slot, ItemStack soul, long playerXp) {
        if (!soul.is(ModItems.SOUL_FLASK)) return false;
        long stored;
        try { stored = FlaskStoredXp.get(soul); } catch (IllegalStateException ignored) { return false; }
        SoulFlaskCapturePlan plan = SoulFlaskCapturePlan.create(playerXp, stored, ((FlaskItem) ModItems.SOUL_FLASK).capacity());
        if (!plan.capturedAnything()) return false;
        ItemStack spent = soul.transmuteCopy(ModItems.FLASK_III, 1);
        FlaskStoredXp.set(spent, plan.resultingStoredXp());
        FlaskVisuals.refresh(spent);
        if (hand != null) player.setItemInHand(hand, spent);
        else player.getInventory().setItem(slot, spent);
        PlayerXp.applyExactRawXp(player, playerXp, plan.remainingPlayerXp());
        return true;
    }
}

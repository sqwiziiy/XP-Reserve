package com.mentality.xpreserve.item;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.xp.PlayerXp;
import com.mentality.xpreserve.xp.XpMath;
import com.mentality.xpreserve.xp.XpTransaction;
import com.mentality.xpreserve.xp.XpTransfer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reusable, server-authoritative experience flask. */
public final class FlaskItem extends Item {
    public static final long CAPACITY = XpMath.totalXpAtLevel(30);

    public FlaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        final long storedXp;
        try {
            storedXp = FlaskStoredXp.get(stack);
        } catch (IllegalStateException exception) {
            XpReserveMod.LOGGER.warn("Refusing XP Reserve transfer for malformed Flask I state: {}", exception.getMessage());
            return InteractionResult.FAIL;
        }

        long playerXp = PlayerXp.currentRawXp(player);
        XpTransaction transaction = player.isShiftKeyDown()
                ? XpTransfer.deposit(playerXp, storedXp, CAPACITY)
                : XpTransfer.withdraw(playerXp, storedXp, CAPACITY);

        if (!transaction.changed()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!PlayerXp.canRepresentRawXp(transaction.newPlayerRawXp())) {
            XpReserveMod.LOGGER.warn("Refusing Flask I withdrawal because player XP would exceed vanilla's integer range");
            return InteractionResult.FAIL;
        }

        // Both writes are validated before mutation. The component write cannot fail after
        // validation, and the bridge guards vanilla's bounded integer XP representation.
        FlaskStoredXp.set(stack, transaction.newStoredRawXp());
        PlayerXp.applyExactRawXp(player, transaction.oldPlayerRawXp(), transaction.newPlayerRawXp());
        return InteractionResult.SUCCESS_SERVER;
    }
}

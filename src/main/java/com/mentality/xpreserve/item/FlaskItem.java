package com.mentality.xpreserve.item;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.xp.PlayerXp;
import com.mentality.xpreserve.xp.XpTransaction;
import com.mentality.xpreserve.xp.XpTransfer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;

/** Reusable, server-authoritative experience flask. */
public final class FlaskItem extends Item {
    private final FlaskTier tier;

    public FlaskItem(FlaskTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public FlaskTier tier() {
        return tier;
    }

    public long capacity() {
        return tier.capacity();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        try {
            return FlaskStoredXp.get(stack) == capacity();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        final long storedXp;
        try {
            storedXp = FlaskStoredXp.get(stack);
        } catch (IllegalStateException ignored) {
            tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        FlaskFillState fill = FlaskFillState.from(storedXp, capacity());
        tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.stored", storedXp, capacity()).withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.capacity", tier.equivalentLevel()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.xp-reserve.flask." + fill.name().toLowerCase()).withStyle(ChatFormatting.DARK_GRAY));
        if (fill != FlaskFillState.EMPTY && fill != FlaskFillState.FULL) {
            tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.percent", FlaskFillState.percentage(storedXp, capacity()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.store").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.xp-reserve.flask.withdraw").withStyle(ChatFormatting.DARK_GRAY));
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
            XpReserveMod.LOGGER.warn("Refusing XP Reserve transfer for malformed {} flask state: {}", tier, exception.getMessage());
            return InteractionResult.FAIL;
        }

        long playerXp = PlayerXp.currentRawXp(player);
        XpTransaction transaction = player.isShiftKeyDown()
                ? XpTransfer.deposit(playerXp, storedXp, capacity())
                : XpTransfer.withdraw(playerXp, storedXp, capacity());

        if (!transaction.changed()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!PlayerXp.canRepresentRawXp(transaction.newPlayerRawXp())) {
            XpReserveMod.LOGGER.warn("Refusing {} flask withdrawal because player XP would exceed vanilla's integer range", tier);
            return InteractionResult.FAIL;
        }

        // Both writes are validated before mutation. The component write cannot fail after
        // validation, and the bridge guards vanilla's bounded integer XP representation.
        FlaskStoredXp.set(stack, transaction.newStoredRawXp());
        PlayerXp.applyExactRawXp(player, transaction.oldPlayerRawXp(), transaction.newPlayerRawXp());
        return InteractionResult.SUCCESS_SERVER;
    }
}

package com.mentality.xpreserve.item;

import com.mentality.xpreserve.XpReserveMod;
import com.mentality.xpreserve.registry.ModItems;
import com.mentality.xpreserve.xp.PlayerXp;
import com.mentality.xpreserve.xp.XpTransaction;
import com.mentality.xpreserve.xp.XpTransfer;
import com.mentality.xpreserve.xp.MendingAwareXpRelease;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

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
        if (stack.is(ModItems.SOUL_FLASK)) {
            tooltip.accept(Component.translatable("tooltip.xp-reserve.soul.ready").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable("tooltip.xp-reserve.soul.death").withStyle(ChatFormatting.DARK_GRAY));
        }
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

        if (!player.isShiftKeyDown()) {
            if (storedXp == 0 || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS_SERVER;
            MendingAwareXpRelease.Result release = MendingAwareXpRelease.release(serverPlayer, storedXp);
            if (release == null) return InteractionResult.FAIL;
            FlaskStoredXp.set(stack, 0);
            return InteractionResult.SUCCESS_SERVER;
        }

        long playerXp = PlayerXp.currentRawXp(player);
        XpTransaction transaction = XpTransfer.deposit(playerXp, storedXp, capacity());

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
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                depositSoundFor(stack.is(ModItems.SOUL_FLASK)), SoundSource.PLAYERS, 0.45F, 0.9F);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Soul deposits use a restrained, non-death soul/anchor charge instead of bottle fill. */
    static net.minecraft.sounds.SoundEvent depositSoundFor(boolean soulFlask) {
        return soulFlask ? SoundEvents.RESPAWN_ANCHOR_CHARGE : SoundEvents.BOTTLE_FILL;
    }
}

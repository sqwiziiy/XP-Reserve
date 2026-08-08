package com.mentality.xpreserve.mixin;

import com.mentality.xpreserve.death.SoulFlaskDeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after actual death is entered, immediately before vanilla computes/drops death loot and XP. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerDeathMixin {
    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void xpReserve$captureSoulFlask(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.getAbilities().instabuild && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            SoulFlaskDeathCapture.capture(player);
        }
    }
}

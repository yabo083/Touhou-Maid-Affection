package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.LapPillowClientState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLapPillowSleepMixin {
    @Inject(method = "isSleeping", at = @At("HEAD"), cancellable = true)
    private void touhou_maid_affection$bridgeLapPillowSleepingState(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.renderingDepth > 0
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getBedOrientation", at = @At("HEAD"), cancellable = true)
    private void touhou_maid_affection$bridgeLapPillowBedOrientation(CallbackInfoReturnable<Direction> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(LapPillowClientState.resolveSleepDirection(player));
        }
    }
}

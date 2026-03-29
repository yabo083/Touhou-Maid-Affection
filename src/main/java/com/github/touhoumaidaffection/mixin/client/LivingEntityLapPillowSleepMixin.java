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
    @Inject(method = "m_5803_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$bridgeLapPillowSleepingState(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.renderingDepth > 0
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "m_21259_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$bridgeLapPillowBedOrientation(CallbackInfoReturnable<Direction> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.renderingDepth > 0
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(LapPillowClientState.resolveSleepDirection(player));
        }
    }

    @Inject(method = "m_21256_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$disableFallFlyingForLapPillowRender(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.renderingDepth > 0
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "m_21255_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$disableFallFlyingForLapPillowState(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "m_6067_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$disableVisualSwimmingForLapPillowRender(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.renderingDepth > 0
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "m_6069_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$disableSwimmingForLapPillowState(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(false);
        }
    }
}

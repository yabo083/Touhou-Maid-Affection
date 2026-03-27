package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.LapPillowClientState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityLapPillowPassengerMixin {
    private static final ThreadLocal<Boolean> touhou_maid_affection$RECURSION_GUARD = ThreadLocal.withInitial(() -> false);

    @Inject(method = "m_20159_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$isPassenger(CallbackInfoReturnable<Boolean> cir) {
        if (touhou_maid_affection$RECURSION_GUARD.get()
                || LapPillowClientState.renderingDepth <= 0
                || !((Object) this instanceof AbstractClientPlayer player)) {
            return;
        }

        touhou_maid_affection$RECURSION_GUARD.set(true);
        try {
            if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
                cir.setReturnValue(false);
            }
        } finally {
            touhou_maid_affection$RECURSION_GUARD.set(false);
        }
    }

    @Inject(method = "m_20202_", at = @At("HEAD"), cancellable = true, remap = false)
    private void touhou_maid_affection$getVehicle(CallbackInfoReturnable<Entity> cir) {
        if (touhou_maid_affection$RECURSION_GUARD.get()
                || LapPillowClientState.renderingDepth <= 0
                || !((Object) this instanceof AbstractClientPlayer player)) {
            return;
        }

        touhou_maid_affection$RECURSION_GUARD.set(true);
        try {
            if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
                cir.setReturnValue(null);
            }
        } finally {
            touhou_maid_affection$RECURSION_GUARD.set(false);
        }
    }
}

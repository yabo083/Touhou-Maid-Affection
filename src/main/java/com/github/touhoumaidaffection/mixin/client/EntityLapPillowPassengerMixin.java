package com.github.touhoumaidaffection.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.player.AbstractClientPlayer;
import com.github.touhoumaidaffection.client.LapPillowClientState;

@Mixin(Entity.class)
public abstract class EntityLapPillowPassengerMixin {
    private static final ThreadLocal<Boolean> lapPillow$RECURSION_GUARD = ThreadLocal.withInitial(() -> false);

    @Inject(method = "isPassenger", at = @At("HEAD"), cancellable = true)
    private void touhou_maid_affection$isPassenger(CallbackInfoReturnable<Boolean> cir) {
        if (lapPillow$RECURSION_GUARD.get() || LapPillowClientState.renderingDepth <= 0 || !((Object) this instanceof AbstractClientPlayer player)) return;
        
        lapPillow$RECURSION_GUARD.set(true);
        try {
            if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
                cir.setReturnValue(false);
            }
        } finally {
            lapPillow$RECURSION_GUARD.set(false);
        }
    }

    @Inject(method = "getVehicle", at = @At("HEAD"), cancellable = true)
    private void touhou_maid_affection$getVehicle(CallbackInfoReturnable<Entity> cir) {
        if (lapPillow$RECURSION_GUARD.get() || LapPillowClientState.renderingDepth <= 0 || !((Object) this instanceof AbstractClientPlayer player)) return;

        lapPillow$RECURSION_GUARD.set(true);
        try {
            if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
                cir.setReturnValue(null);
            }
        } finally {
            lapPillow$RECURSION_GUARD.set(false);
        }
    }
}
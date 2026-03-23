
package com.github.touhoumaidaffection.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.player.AbstractClientPlayer;
import com.github.touhoumaidaffection.client.LapPillowClientState;

@Mixin(Entity.class)
public abstract class EntityLapPillowSitMixin {
    @Inject(method = "shouldRiderSit", at = @At("HEAD"), cancellable = true, remap = false)
    private void lapPillowShouldRiderSit(CallbackInfoReturnable<Boolean> cir) {
        Entity seat = (Entity) (Object) this;
        if (LapPillowClientState.isLapPillowSeat(seat)) {
            if (seat.getFirstPassenger() instanceof AbstractClientPlayer player) {
                if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}

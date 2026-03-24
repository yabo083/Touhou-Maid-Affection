package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.LapPillowClientState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LapPillowSeatRenderMixin {
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"),
            require = 0
    )
    private boolean touhou_maid_affection$overridePassengerForSleepPoseBridge(LivingEntity entity) {
        if (entity instanceof AbstractClientPlayer player && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            return false;
        }
        return entity.isPassenger();
    }
}

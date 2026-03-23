package com.github.touhoumaidaffection.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class LapPillowSeatRenderMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private boolean touhou_maid_affection$overrideShouldSit(boolean shouldSit) {
        // This mixin doesn't actually work well either since this is in the base class
        // We'll need a different approach
        return shouldSit;
    }
}

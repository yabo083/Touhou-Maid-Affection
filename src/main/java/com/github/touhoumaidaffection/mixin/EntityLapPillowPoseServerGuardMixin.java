package com.github.touhoumaidaffection.mixin;

import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityLapPillowPoseServerGuardMixin {
    @Inject(method = "m_20124_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$guardServerPose(Pose pose, CallbackInfo ci) {
        if (pose == Pose.SLEEPING || !((Object) this instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (LapPillowState.isActive(serverPlayer) && LapPillowState.getPose(serverPlayer).playerLying()) {
            ci.cancel();
        }
    }
}

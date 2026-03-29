package com.github.touhoumaidaffection.mixin;

import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerLapPillowForcedPoseServerGuardMixin {
    @Inject(method = "setForcedPose", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void touhou_maid_affection$guardServerForcedPose(Pose pose, CallbackInfo ci) {
        if (pose == Pose.SLEEPING || !((Object) this instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (LapPillowState.isActive(serverPlayer) && LapPillowState.getPose(serverPlayer).playerLying()) {
            ci.cancel();
        }
    }
}

package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.LapPillowClientState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityLapPillowPoseClientGuardMixin {
    @Inject(method = "m_20124_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$guardClientPose(Pose pose, CallbackInfo ci) {
        guardClientPoseWrite(pose, ci);
    }

    @Inject(method = "m_217003_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$bridgeLapPillowSleepingHasPose(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if (pose == Pose.SLEEPING
                && (Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "m_20089_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$bridgeLapPillowSleepingGetPose(CallbackInfoReturnable<Pose> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(Pose.SLEEPING);
        }
    }

    @Inject(method = "m_20096_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void touhou_maid_affection$bridgeLapPillowGroundedState(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractClientPlayer player
                && LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            cir.setReturnValue(true);
        }
    }

    private void guardClientPoseWrite(Pose pose, CallbackInfo ci) {
        if (pose == Pose.SLEEPING || !((Object) this instanceof AbstractClientPlayer player)) {
            return;
        }
        if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            ci.cancel();
        }
    }
}

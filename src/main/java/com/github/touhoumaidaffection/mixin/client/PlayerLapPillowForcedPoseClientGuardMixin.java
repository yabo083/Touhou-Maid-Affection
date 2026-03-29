package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.LapPillowClientState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerLapPillowForcedPoseClientGuardMixin {
    @Inject(method = "setForcedPose", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void touhou_maid_affection$guardClientForcedPose(Pose pose, CallbackInfo ci) {
        if (pose == Pose.SLEEPING || !((Object) this instanceof AbstractClientPlayer player)) {
            return;
        }
        if (LapPillowClientState.shouldUseSleepPoseBridge(player)) {
            ci.cancel();
        }
    }
}

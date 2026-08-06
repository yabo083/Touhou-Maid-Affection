package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.KissFovHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GMOD-style kiss close-up: dollies the first-person camera position from the
 * player's eye toward the maid's face while the kiss animation is running.
 *
 * <p>{@code ViewportEvent.ComputeCameraAngles} fires before {@code Camera.setup()}
 * overwrites the position, so the yaw/pitch snap keeps working but the position
 * must be applied after setup — hence this TAIL injection.</p>
 */
@Mixin(Camera.class)
public abstract class CameraKissMixin {
    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void touhou_maid_affection$kissCameraDolly(
            BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci
    ) {
        // First-person only: in third person the existing angle snap + FOV zoom stay.
        if (detached) {
            return;
        }
        Vec3 dollyPos = KissFovHandler.getDollyPosition();
        if (dollyPos != null) {
            this.touhou_maid_affection$setPosition(dollyPos);
        }
    }

    @Invoker("setPosition")
    protected abstract void touhou_maid_affection$setPosition(Vec3 position);
}

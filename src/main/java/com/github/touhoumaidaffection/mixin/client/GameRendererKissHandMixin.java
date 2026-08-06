package com.github.touhoumaidaffection.mixin.client;

import com.github.touhoumaidaffection.client.KissFovHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the first-person hand model while the kiss camera-dolly runs, so the
 * player's bare arm does not float in front of the maid's face during the close-up.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererKissHandMixin {
    @Inject(method = "renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void touhou_maid_affection$hideHandDuringKissCloseUp(
            Camera camera, float partialTick, Matrix4f viewMatrix, CallbackInfo ci
    ) {
        if (KissFovHandler.isDollyActive()) {
            ci.cancel();
        }
    }
}

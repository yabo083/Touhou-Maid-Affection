package com.github.touhoumaidaffection.mixin;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMaidContainerGui.class)
public abstract class AbstractMaidContainerGuiMixin {
    @Unique
    private static final ThreadLocal<String> TOUHOU_MAID_AFFECTION$DISPLAY_NAME = new ThreadLocal<>();

    @Shadow
    protected EntityMaid maid;

    @Inject(method = "renderMaidInfo", at = @At("HEAD"))
    private void touhou_maid_affection$captureResolvedName(CallbackInfo ci) {
        TOUHOU_MAID_AFFECTION$DISPLAY_NAME.set(MaidDisplayNameResolver.resolvePlainDisplayName(this.maid));
    }

    @Inject(method = "renderMaidInfo", at = @At("RETURN"))
    private void touhou_maid_affection$clearResolvedName(CallbackInfo ci) {
        TOUHOU_MAID_AFFECTION$DISPLAY_NAME.remove();
    }

    @Redirect(
            method = "lambda$renderMaidInfo$10",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/tartaricacid/touhoulittlemaid/client/resource/pojo/MaidModelInfo;getName()Ljava/lang/String;"
            )
    )
    private static String touhou_maid_affection$replaceModelName(MaidModelInfo instance) {
        String resolved = TOUHOU_MAID_AFFECTION$DISPLAY_NAME.get();
        if (resolved != null && !resolved.isBlank()) {
            return resolved;
        }
        return instance.getName();
    }
}

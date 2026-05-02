package com.github.touhoumaidaffection.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.LLMSiteEditorScreen;
import com.github.touhoumaidaffection.ai.mimo.MimoLLMSite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LLMSiteEditorScreen.class)
public abstract class LLMSiteEditorScreenMixin {
    @Shadow
    @Final
    private LLMSite sourceSite;

    @Inject(method = "buildSite", at = @At("RETURN"), cancellable = true, remap = false)
    private void touhou_maid_affection$preserveMimoSiteType(CallbackInfoReturnable<LLMSite> cir) {
        LLMSite builtSite = cir.getReturnValue();
        if (this.sourceSite instanceof MimoLLMSite && builtSite instanceof LLMOpenAISite openAISite) {
            cir.setReturnValue(MimoLLMSite.fromOpenAISite(openAISite));
        }
    }
}

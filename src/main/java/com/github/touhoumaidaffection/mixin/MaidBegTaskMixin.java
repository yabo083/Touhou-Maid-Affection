package com.github.touhoumaidaffection.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidBegTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MaidBegTask.class)
public class MaidBegTaskMixin {

    @Inject(method = "holdTemptationItem", at = @At("RETURN"), cancellable = true, remap = false)
    private void onHoldTemptationItem(EntityMaid owner, LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            // If main hand check failed, also check offhand
            if (owner.getTemptationItem().test(e.getOffhandItem())) {
                cir.setReturnValue(true);
            }
        }
    }
}

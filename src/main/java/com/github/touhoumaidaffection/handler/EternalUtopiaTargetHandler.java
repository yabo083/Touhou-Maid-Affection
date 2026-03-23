package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.ModEffects;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class EternalUtopiaTargetHandler {
    private EternalUtopiaTargetHandler() {
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity nextTarget = event.getNewAboutToBeSetTarget();
        if (nextTarget == null || !nextTarget.hasEffect(ModEffects.ETERNAL_UTOPIA.getDelegate())) {
            return;
        }
        event.setNewAboutToBeSetTarget(null);
    }
}

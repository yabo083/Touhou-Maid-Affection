package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.ModEffects;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class EternalUtopiaTargetHandler {
    private EternalUtopiaTargetHandler() {
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity nextTarget = event.getNewTarget();
        if (nextTarget == null || !nextTarget.hasEffect(ModEffects.ETERNAL_UTOPIA.get())) {
            return;
        }
        event.setNewTarget(null);
    }
}

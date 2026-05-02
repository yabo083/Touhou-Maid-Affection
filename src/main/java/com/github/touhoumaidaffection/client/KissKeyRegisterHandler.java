package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KissKeyRegisterHandler {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KissKeyMappings.KISS_CARRIED_MAID);
        event.register(KissKeyMappings.KISS_TARGETED_MAID);
        event.register(BondKeyMappings.LAP_PILLOW);
        event.register(BondKeyMappings.LAP_PILLOW_ANGLE_LOCK);
        event.register(BondKeyMappings.VOICE_PREVIEW);
    }
}

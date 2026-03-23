package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.ModEntityTypes;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LapPillowAnchorRendererHandler {
    private LapPillowAnchorRendererHandler() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.LAP_PILLOW_ANCHOR.get(), LapPillowAnchorRenderer::new);
    }
}

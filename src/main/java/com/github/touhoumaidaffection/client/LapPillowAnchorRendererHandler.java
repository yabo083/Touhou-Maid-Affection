package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.ModEntityTypes;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LapPillowAnchorRendererHandler {
    private LapPillowAnchorRendererHandler() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.LAP_PILLOW_ANCHOR.get(), LapPillowAnchorRenderer::new);
    }
}

package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.network.KissMaidPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

public class KissClientHandler {
    public static void handle(KissMaidPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Entity maid = level.getEntity(payload.maidEntityId());
        Entity player = level.getEntity(payload.playerEntityId());
        if (maid == null || player == null) {
            return;
        }

        // Trigger FOV zoom if this is the local player
        if (player == Minecraft.getInstance().player && ModConfig.FOV_ZOOM_ENABLED.get()) {
            KissFovHandler.trigger(maid.getId(), payload.carriedKiss());
        }
        KissParticleEffectManager.queueKissEffect(maid, player);
    }
}

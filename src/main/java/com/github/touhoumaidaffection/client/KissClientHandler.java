package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.network.KissMaidPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class KissClientHandler {
    public static void handle(KissMaidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;

            Entity maid = level.getEntity(payload.maidEntityId());
            Entity player = level.getEntity(payload.playerEntityId());
            if (maid == null || player == null) return;

            // Trigger kiss camera animation if this is the local player
            if (payload.allowFovZoom() && player == Minecraft.getInstance().player) {
                boolean carriedKiss = maid.getVehicle() == player;
                KissFovHandler.trigger(maid.getId(), carriedKiss);
            }

            KissParticleEffectManager.queueKissEffect(maid, player);
        });
    }
}

package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.network.KissTargetedMaidRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class KissTargetedMaidRequestHandler {
    private static final double MAX_KEY_KISS_DISTANCE_SQR = 36.0D;

    public static void handle(KissTargetedMaidRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(payload.maidEntityId());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
                return;
            }
            if (player.distanceToSqr(maid) > MAX_KEY_KISS_DISTANCE_SQR || !player.hasLineOfSight(maid)) {
                return;
            }
            KissMaidHandler.performKiss(player, maid);
        });
    }
}

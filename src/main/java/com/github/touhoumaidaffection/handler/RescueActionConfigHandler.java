package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.network.RescueActionConfigPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RescueActionConfigHandler {
    private RescueActionConfigHandler() {
    }

    public static void handle(RescueActionConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                BondManager.setMaidRescueAction(player, payload.maidUuid(), payload.actionId());
            }
        });
    }
}

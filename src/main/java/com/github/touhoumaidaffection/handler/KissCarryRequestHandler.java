package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class KissCarryRequestHandler {
    public static void handle(KissCarryRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KissMaidHandler.tryKissCarriedMaid(player);
            }
        });
    }
}

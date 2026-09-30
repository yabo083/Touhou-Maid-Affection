package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.service.RandomGiftService;
import com.github.touhoumaidaffection.network.TmaGiftStatusPayload;
import com.github.touhoumaidaffection.network.TmaGiftStatusRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraftforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TmaGiftStatusRequestHandler {
    private TmaGiftStatusRequestHandler() { }

    public static void handleStatusRequest(TmaGiftStatusRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new TmaGiftStatusPayload(RandomGiftService.giftStatus(player, payload.page())));
            }
        });
    }
}

package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.network.BondStateRequestPayload;
import com.github.touhoumaidaffection.compat.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

public final class BondStateRequestHandler {
    private BondStateRequestHandler() {
    }

    public static void handle(BondStateRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            var maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null) {
                return;
            }
            BondManager.setBondLevel(player, maid.getUUID(), maid.getFavorabilityManager().getLevel());
            BondManager.syncMaidProfile(player, maid);
            BondSyncHelper.sendBondState(player, maid);
        });
    }
}

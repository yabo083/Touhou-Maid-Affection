package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.network.RescueActionConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RescueActionConfigHandler {
    private RescueActionConfigHandler() {
    }

    public static void handle(RescueActionConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null || !BondManager.isAbilityUnlocked(player, payload.maidUuid(), "emergency_heal")) {
                return;
            }
            BondManager.setMaidRescueAction(player, maid.getUUID(), payload.actionId());
        });
    }
}

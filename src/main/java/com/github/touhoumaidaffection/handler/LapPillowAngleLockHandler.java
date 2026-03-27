package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.network.LapPillowAngleLockPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class LapPillowAngleLockHandler {
    private LapPillowAngleLockHandler() {
    }

    public static void handle(LapPillowAngleLockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !LapPillowState.isActive(player)) {
                return;
            }
            if (payload.enabled()) {
                LapPillowState.enableAngleLock(player, payload.lockedYaw());
            } else {
                LapPillowState.disableAngleLock(player);
            }
            TouhouMaidAffection.LOGGER.info(
                    "[LapPillow] Angle lock update: player={} enabled={} yaw={}",
                    player.getScoreboardName(),
                    payload.enabled(),
                    String.format(java.util.Locale.ROOT, "%.2f", payload.lockedYaw())
            );
        });
    }
}

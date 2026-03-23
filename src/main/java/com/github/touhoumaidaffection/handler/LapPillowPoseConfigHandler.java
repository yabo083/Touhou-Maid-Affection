package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.network.LapPillowPoseConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class LapPillowPoseConfigHandler {
    private LapPillowPoseConfigHandler() {
    }

    public static void handle(LapPillowPoseConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !BondManager.isAbilityUnlocked(player, payload.maidUuid(), "lap_pillow")) {
                return;
            }

            LapPillowPoseSnapshot pose = new LapPillowPoseSnapshot(
                    LapPillowMode.fromName(payload.mode()),
                    payload.maidOffsetX(),
                    payload.maidOffsetY(),
                    payload.maidOffsetZ(),
                    payload.playerOffsetX(),
                    payload.playerOffsetY(),
                    payload.playerOffsetZ(),
                    payload.maidActionId(),
                    payload.playerActionId()
            ).clamp();
            BondManager.setMaidLapPillowPose(player, maid.getUUID(), pose);
            if (LapPillowState.isActive(player) && maid.getUUID().equals(LapPillowState.getMaidUuid(player))) {
                LapPillowState.updatePose(player, pose);
            }
            TouhouMaidAffection.LOGGER.info(
                    "[LapPillow] Pose saved: player={} maid={} mode={} maidOffset=({}, {}, {}) playerOffset=({}, {}, {}) maidAction='{}' playerAction='{}'",
                    player.getScoreboardName(),
                    maid.getUUID(),
                    pose.mode().serializedName(),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.maidOffsetX()),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.maidOffsetY()),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.maidOffsetZ()),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.playerOffsetX()),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.playerOffsetY()),
                    String.format(java.util.Locale.ROOT, "%.3f", pose.playerOffsetZ()),
                    pose.maidActionId(),
                    pose.playerActionId()
            );
            BondSyncHelper.sendBondState(player, maid);
        });
    }
}

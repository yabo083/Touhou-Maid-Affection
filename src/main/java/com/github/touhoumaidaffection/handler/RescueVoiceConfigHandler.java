package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.network.RescueVoiceConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RescueVoiceConfigHandler {
    private RescueVoiceConfigHandler() {
    }

    public static void handle(RescueVoiceConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null || !BondManager.isAbilityUnlocked(player, payload.maidUuid(), "emergency_heal")) {
                return;
            }
            EmergencyRescueVoiceSettings settings = EmergencyRescueVoiceSettings.of(
                    payload.sourceMode(),
                    payload.tlmPlayMode(),
                    payload.tlmSelectedGroup(),
                    payload.tlmSelectedClip(),
                    payload.customPlayMode(),
                    payload.fixedFile(),
                    payload.useCommonFallback(),
                    payload.selectedVoiceIds()
            );
            BondManager.setEmergencyRescueVoiceSettings(player, maid.getUUID(), settings);
            BondManager.syncMaidProfile(player, maid);
        });
    }
}

package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MorningKissVoiceConfigHandler {
    private MorningKissVoiceConfigHandler() {
    }

    public static void handle(MorningKissVoiceConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null || !BondManager.isAbilityUnlocked(player, payload.maidUuid(), "morning_kiss")) {
                return;
            }
            BondManager.setMorningKissVoiceSettings(player, maid.getUUID(),
                    MorningKissVoiceSettings.of(payload.mode(), payload.selectedGroup(), payload.selectedClip(), payload.soundPackId(), payload.selectedVoiceIds()));
            BondManager.syncMaidProfile(player, maid);
        });
    }
}

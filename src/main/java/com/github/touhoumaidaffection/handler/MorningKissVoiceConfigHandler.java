package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MorningKissVoiceConfigHandler {
    private MorningKissVoiceConfigHandler() {
    }

    public static void handle(MorningKissVoiceConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isOwnedBy(player)) {
                return;
            }
            BondManager.setMorningKissVoiceSettings(player, maid.getUUID(),
                    MorningKissVoiceSettings.of(payload.mode(), payload.selectedGroup(), payload.selectedClip(), payload.soundPackId()));
            BondManager.syncMaidProfile(player, maid);
        });
    }
}

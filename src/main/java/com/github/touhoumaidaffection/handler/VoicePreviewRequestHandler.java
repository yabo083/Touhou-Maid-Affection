package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.service.InteractionVoiceProfileData;
import com.github.touhoumaidaffection.network.VoicePreviewDataPackPlayPayload;
import com.github.touhoumaidaffection.network.VoicePreviewRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public final class VoicePreviewRequestHandler {
    private VoicePreviewRequestHandler() {
    }

    public static void handle(VoicePreviewRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !VoicePoolIds.isDataPack(payload.voiceId())) {
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null) {
                return;
            }
            Optional<InteractionVoiceProfileData.DataPackVoice> voice = resolveVoice(player, maid, payload);
            voice.ifPresent(dataPackVoice -> TouhouMaidAffection.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new VoicePreviewDataPackPlayPayload(
                            maid.getId(),
                            maid.getUUID(),
                            payload.feature(),
                            dataPackVoice.fileName(),
                            dataPackVoice.data()
                    )
            ));
        });
    }

    private static Optional<InteractionVoiceProfileData.DataPackVoice> resolveVoice(ServerPlayer player, EntityMaid maid, VoicePreviewRequestPayload payload) {
        String fileName = VoicePoolIds.value(payload.voiceId());
        if (VoicePreviewRequestPayload.FEATURE_MORNING_KISS.equals(payload.feature())) {
            if (!BondManager.isAbilityUnlocked(player, maid.getUUID(), "morning_kiss")) {
                return Optional.empty();
            }
            return InteractionVoiceProfileData.selectVoiceByFile(
                    InteractionVoiceProfileData.resolveMorningKiss(maid),
                    fileName
            );
        }
        if (VoicePreviewRequestPayload.FEATURE_EMERGENCY_RESCUE.equals(payload.feature())) {
            if (!BondManager.isAbilityUnlocked(player, maid.getUUID(), "emergency_heal")) {
                return Optional.empty();
            }
            return InteractionVoiceProfileData.selectVoiceByFile(
                    InteractionVoiceProfileData.resolveEmergencyRescue(
                            maid.getUUID().toString(),
                            BondData.of(player).getMaidProfile(maid.getUUID())
                    ),
                    fileName
            );
        }
        return Optional.empty();
    }
}

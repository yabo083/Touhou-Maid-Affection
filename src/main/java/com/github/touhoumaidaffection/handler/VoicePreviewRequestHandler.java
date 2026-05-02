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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public final class VoicePreviewRequestHandler {
    private VoicePreviewRequestHandler() {
    }

    public static void handle(VoicePreviewRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !VoicePoolIds.isDataPack(payload.voiceId())) {
                TouhouMaidAffection.LOGGER.info("Voice preview request ignored: player={}, voiceId={}",
                        context.player() == null ? "null" : context.player().getName().getString(), payload.voiceId());
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null) {
                TouhouMaidAffection.LOGGER.info("Voice preview request denied: player={}, maidUuid={} not owned/reachable",
                        player.getName().getString(), payload.maidUuid());
                return;
            }
            Optional<InteractionVoiceProfileData.DataPackVoice> voice = resolveVoice(player, maid, payload);
            if (voice.isEmpty()) {
                TouhouMaidAffection.LOGGER.info("Voice preview request denied: player={}, maidUuid={}, feature={}, voiceId={} unresolved",
                        player.getName().getString(), payload.maidUuid(), payload.feature(), payload.voiceId());
                return;
            }
            InteractionVoiceProfileData.DataPackVoice dataPackVoice = voice.get();
            TouhouMaidAffection.LOGGER.info("Voice preview request accepted: player={}, maidUuid={}, feature={}, file={}, bytes={}",
                    player.getName().getString(), payload.maidUuid(), payload.feature(), dataPackVoice.fileName(), dataPackVoice.data().length);
            PacketDistributor.sendToPlayer(player, new VoicePreviewDataPackPlayPayload(
                    maid.getId(),
                    maid.getUUID(),
                    payload.feature(),
                    dataPackVoice.fileName(),
                    dataPackVoice.data()
            ));
        });
    }

    private static Optional<InteractionVoiceProfileData.DataPackVoice> resolveVoice(ServerPlayer player, EntityMaid maid,
                                                                                   VoicePreviewRequestPayload payload) {
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

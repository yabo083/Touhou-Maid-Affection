package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;

public final class BondClientPayloadHandler {
    private BondClientPayloadHandler() {
    }

    public static void handleBondStateSync(BondStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BondClientStateCache.update(
                payload.maidUuid(),
                new LinkedHashSet<>(payload.unlockedAbilityIds()),
                payload.queuedGiftCount(),
                payload.maxQueuedGiftCount(),
                payload.nextGiftReadySeconds(),
                MorningKissVoiceSettings.of(
                        payload.morningKissVoiceMode(),
                        payload.morningKissVoiceGroup(),
                        payload.morningKissVoiceClip(),
                        payload.morningKissVoicePack()
                ),
                new LapPillowPoseSnapshot(
                        com.github.touhoumaidaffection.bond.lap.LapPillowMode.fromName(payload.lapPillowMode()),
                        payload.lapPillowMaidOffsetX(),
                        payload.lapPillowMaidOffsetY(),
                        payload.lapPillowMaidOffsetZ(),
                        payload.lapPillowPlayerOffsetX(),
                        payload.lapPillowPlayerOffsetY(),
                        payload.lapPillowPlayerOffsetZ(),
                        payload.lapPillowMaidAction(),
                        payload.lapPillowPlayerAction()
                )
        ));
    }

    public static void handleRescuePop(MaidRescuePopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueVisualHandler.play(payload));
    }

    public static void handleMorningKissVoicePlay(MorningKissVoicePlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MorningKissVoicePlayback.play(payload));
    }
}

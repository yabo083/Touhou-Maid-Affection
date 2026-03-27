package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundReloadPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncChunkPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncClearPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncCompletePayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncManifestPayload;
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
                EmergencyRescueVoiceSettings.of(
                        payload.rescueVoiceSourceMode(),
                        payload.rescueVoiceTlmMode(),
                        payload.rescueVoiceTlmGroup(),
                        payload.rescueVoiceTlmClip(),
                        payload.rescueVoiceCustomPlayMode(),
                        payload.rescueVoiceFixedFile(),
                        payload.rescueVoiceUseCommonFallback()
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

    public static void handleRescueSoundSyncManifest(RescueSoundSyncManifestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueServerSoundSyncClient.handleManifest(payload));
    }

    public static void handleRescueSoundSyncClear(RescueSoundSyncClearPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueServerSoundSyncClient.handleClear(payload));
    }

    public static void handleRescueSoundSyncChunk(RescueSoundSyncChunkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueServerSoundSyncClient.handleChunk(payload));
    }

    public static void handleRescueSoundSyncComplete(RescueSoundSyncCompletePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueServerSoundSyncClient.handleComplete(payload));
    }

    public static void handleRescueSoundReload(RescueSoundReloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EmergencyRescueServerSoundSyncClient.handleReload(payload));
    }
}

package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.github.touhoumaidaffection.network.MorningKissDataVoicePlayPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.github.touhoumaidaffection.network.TmaSettingsStatePayload;
import com.github.touhoumaidaffection.network.VoicePreviewDataPackPlayPayload;
import com.github.touhoumaidaffection.network.VoicePreviewThrottledPayload;
import net.minecraft.network.chat.Component;
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
                        payload.morningKissVoicePack(),
                        payload.morningKissSelectedVoiceIds()
                ),
                EmergencyRescueVoiceSettings.of(
                        payload.rescueVoiceSourceMode(),
                        payload.rescueVoiceTlmMode(),
                        payload.rescueVoiceTlmGroup(),
                        payload.rescueVoiceTlmClip(),
                        payload.rescueVoiceCustomPlayMode(),
                        payload.rescueVoiceFixedFile(),
                        payload.rescueVoiceUseCommonFallback(),
                        payload.rescueSelectedVoiceIds()
                ),
                payload.morningKissDataPackVoiceMode(),
                payload.morningKissDataPackVoiceFiles(),
                payload.rescueDataPackVoiceMode(),
                payload.rescueDataPackVoiceFiles(),
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

    public static void handleMorningKissDataVoicePlay(MorningKissDataVoicePlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MorningKissVoicePlayback.playDataPackVoice(payload));
    }

    public static void handleVoicePreviewDataPackPlay(VoicePreviewDataPackPlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VoicePreviewPlayback.playDataPackVoice(payload));
    }

    public static void handleVoicePreviewThrottled(VoicePreviewThrottledPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                context.player().displayClientMessage(Component.translatable("bond.voice_preview.throttled"), true);
            }
        });
    }

    public static void handleSettingsState(TmaSettingsStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TmaSettingsClientState.applyState(payload));
    }
}

package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.bond.service.InteractionVoiceProfileData;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BondSyncHelper {
    private BondSyncHelper() {
    }

    public static void sendBondState(ServerPlayer player, EntityMaid maid) {
        long nowMs = System.currentTimeMillis();
        int queuedGiftCount = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                ? BondManager.reconcileRandomGiftQueue(player, maid.getUUID(), nowMs)
                : 0;
        long nextGiftReadyAtMs = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                ? BondManager.getNextRandomGiftReadyAtMs(player, maid.getUUID(), nowMs)
                : 0L;
        int nextGiftReadySeconds = nextGiftReadyAtMs > nowMs
                ? (int) Math.min(Integer.MAX_VALUE, (nextGiftReadyAtMs - nowMs + 999L) / 1000L)
                : 0;
        MorningKissVoiceSettings voiceSettings = BondManager.getMorningKissVoiceSettings(player, maid.getUUID())
                .withSoundPackId(maid.getSoundPackId());
        EmergencyRescueVoiceSettings rescueVoiceSettings = BondManager.getEmergencyRescueVoiceSettings(player, maid.getUUID());
        LapPillowPoseSnapshot lapPillowPose = BondManager.getMaidLapPillowPose(player, maid.getUUID()).clamp();
        InteractionVoiceProfileData.ResolvedVoiceProfile morningProfile = InteractionVoiceProfileData.resolveMorningKiss(maid);
        InteractionVoiceProfileData.ResolvedVoiceProfile rescueProfile =
                InteractionVoiceProfileData.resolveEmergencyRescue(maid.getUUID().toString(), BondData.of(player).getMaidProfile(maid.getUUID()));

        PacketDistributor.sendToPlayer(player, new BondStateSyncPayload(
                maid.getUUID(),
                BondManager.getUnlockedAbilityIds(player, maid.getUUID()),
                queuedGiftCount,
                Math.max(1, ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get()),
                nextGiftReadySeconds,
                voiceSettings.mode().serializedName(),
                voiceSettings.selectedGroup(),
                voiceSettings.selectedClip(),
                voiceSettings.soundPackId(),
                voiceSettings.selectedVoiceIds(),
                morningProfile.voiceMode().name().toLowerCase(java.util.Locale.ROOT),
                morningProfile.fileNames(),
                rescueVoiceSettings.sourceMode().serializedName(),
                rescueVoiceSettings.tlmPlayMode().serializedName(),
                rescueVoiceSettings.tlmSelectedGroup(),
                rescueVoiceSettings.tlmSelectedClip(),
                rescueVoiceSettings.customPlayMode().serializedName(),
                rescueVoiceSettings.fixedFile(),
                rescueVoiceSettings.useCommonFallback(),
                rescueVoiceSettings.selectedVoiceIds(),
                rescueProfile.voiceMode().name().toLowerCase(java.util.Locale.ROOT),
                rescueProfile.fileNames(),
                lapPillowPose.mode().serializedName(),
                lapPillowPose.maidOffsetX(),
                lapPillowPose.maidOffsetY(),
                lapPillowPose.maidOffsetZ(),
                lapPillowPose.playerOffsetX(),
                lapPillowPose.playerOffsetY(),
                lapPillowPose.playerOffsetZ(),
                lapPillowPose.maidActionId(),
                lapPillowPose.playerActionId()
        ));
    }
}

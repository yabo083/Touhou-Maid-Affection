package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.network.BondStateRequestPayload;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BondStateRequestHandler {
    private BondStateRequestHandler() {
    }

    public static void handle(BondStateRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid)) {
                return;
            }
            BondManager.setBondLevel(player, maid.getUUID(), maid.getFavorabilityManager().getLevel());
            BondManager.syncMaidProfile(player, maid);
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
            PacketDistributor.sendToPlayer(player, new BondStateSyncPayload(
                    maid.getUUID(),
                    BondManager.getUnlockedAbilityIds(player, maid.getUUID()),
                    queuedGiftCount,
                    Math.max(1, com.github.touhoumaidaffection.ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get()),
                    nextGiftReadySeconds,
                    voiceSettings.mode().serializedName(),
                    voiceSettings.selectedGroup(),
                    voiceSettings.selectedClip(),
                    voiceSettings.soundPackId()
            ));
        });
    }
}

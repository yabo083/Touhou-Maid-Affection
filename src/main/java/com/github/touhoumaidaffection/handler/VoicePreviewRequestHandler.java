package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.service.InteractionVoiceProfileData;
import com.github.touhoumaidaffection.network.VoicePreviewDataPackPlayPayload;
import com.github.touhoumaidaffection.network.VoicePreviewRequestPayload;
import com.github.touhoumaidaffection.network.VoicePreviewThrottledPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class VoicePreviewRequestHandler {
    /** 数据包语音试听冷却：0.5 秒（10 tick）。 */
    static final long COOLDOWN_TICKS = 10L;
    private static final VoicePreviewRateLimiter RATE_LIMITER = new VoicePreviewRateLimiter(COOLDOWN_TICKS);

    private VoicePreviewRequestHandler() {
    }

    public static void handle(VoicePreviewRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // 非数据包（本地播放）请求不占用限流额度，直接放行给客户端自行处理。
            if (!VoicePoolIds.isDataPack(payload.voiceId())) {
                return;
            }
            if (!RATE_LIMITER.tryAcquire(player.getUUID(), player.getServer().getTickCount())) {
                PacketDistributor.sendToPlayer(player, VoicePreviewThrottledPayload.INSTANCE);
                return;
            }
            EntityMaid maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null) {
                return;
            }
            Optional<InteractionVoiceProfileData.DataPackVoice> voice = resolveVoice(player, maid, payload);
            if (voice.isEmpty()) {
                return;
            }
            InteractionVoiceProfileData.DataPackVoice dataPackVoice = voice.get();
            PacketDistributor.sendToPlayer(player, new VoicePreviewDataPackPlayPayload(
                    maid.getId(),
                    maid.getUUID(),
                    payload.feature(),
                    dataPackVoice.fileName(),
                    dataPackVoice.data()
            ));
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RATE_LIMITER.remove(event.getEntity().getUUID());
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

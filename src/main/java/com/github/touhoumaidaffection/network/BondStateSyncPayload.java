package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BondStateSyncPayload(
        UUID maidUuid,
        List<String> unlockedAbilityIds,
        int queuedGiftCount,
        int maxQueuedGiftCount,
        int nextGiftReadySeconds,
        String morningKissVoiceMode,
        String morningKissVoiceGroup,
        String morningKissVoiceClip,
        String morningKissVoicePack
) implements CustomPacketPayload {
    public static final Type<BondStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "bond_state_sync"));

    public static final StreamCodec<ByteBuf, BondStateSyncPayload> STREAM_CODEC = StreamCodec.of(
            BondStateSyncPayload::encode,
            BondStateSyncPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, BondStateSyncPayload payload) {
        UUIDUtil.STREAM_CODEC.encode(buf, payload.maidUuid());
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                .encode(buf, new ArrayList<>(payload.unlockedAbilityIds()));
        ByteBufCodecs.INT.encode(buf, payload.queuedGiftCount());
        ByteBufCodecs.INT.encode(buf, payload.maxQueuedGiftCount());
        ByteBufCodecs.INT.encode(buf, payload.nextGiftReadySeconds());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.morningKissVoiceMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.morningKissVoiceGroup());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.morningKissVoiceClip());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.morningKissVoicePack());
    }

    private static BondStateSyncPayload decode(ByteBuf buf) {
        return new BondStateSyncPayload(
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf)
        );
    }
}

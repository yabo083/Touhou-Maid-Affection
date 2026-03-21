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
        int nextGiftReadySeconds
) implements CustomPacketPayload {
    public static final Type<BondStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "bond_state_sync"));

    public static final StreamCodec<ByteBuf, BondStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BondStateSyncPayload::maidUuid,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), BondStateSyncPayload::unlockedAbilityIds,
            ByteBufCodecs.INT, BondStateSyncPayload::queuedGiftCount,
            ByteBufCodecs.INT, BondStateSyncPayload::maxQueuedGiftCount,
            ByteBufCodecs.INT, BondStateSyncPayload::nextGiftReadySeconds,
            BondStateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

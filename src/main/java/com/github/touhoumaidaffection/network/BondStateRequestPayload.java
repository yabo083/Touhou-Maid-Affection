package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record BondStateRequestPayload(UUID maidUuid) implements CustomPacketPayload {
    public static final Type<BondStateRequestPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "bond_state_request"));

    public static final StreamCodec<ByteBuf, BondStateRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.UUID,
            BondStateRequestPayload::maidUuid,
            BondStateRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

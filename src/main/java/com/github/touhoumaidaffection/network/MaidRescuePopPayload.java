package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaidRescuePopPayload(String maidModelId) implements CustomPacketPayload {
    public static final Type<MaidRescuePopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "maid_rescue_pop"));

    public static final StreamCodec<ByteBuf, MaidRescuePopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            MaidRescuePopPayload::maidModelId,
            MaidRescuePopPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

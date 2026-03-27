package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record LapPillowStartPayload(UUID maidUuid) implements CustomPacketPayload {
    public static final Type<LapPillowStartPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "lap_pillow_start"));

    public static final StreamCodec<ByteBuf, LapPillowStartPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.UUID,
            LapPillowStartPayload::maidUuid,
            LapPillowStartPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

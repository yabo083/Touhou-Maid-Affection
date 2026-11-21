package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import com.github.touhoumaidaffection.compat.ByteBufCodecs;
import com.github.touhoumaidaffection.compat.StreamCodec;
import com.github.touhoumaidaffection.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LapPillowExitPayload(int reserved) implements CustomPacketPayload {
    public static final Type<LapPillowExitPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "lap_pillow_exit"));

    public static final StreamCodec<ByteBuf, LapPillowExitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LapPillowExitPayload::reserved,
            LapPillowExitPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

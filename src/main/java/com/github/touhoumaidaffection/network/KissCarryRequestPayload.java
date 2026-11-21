package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import com.github.touhoumaidaffection.compat.ByteBufCodecs;
import com.github.touhoumaidaffection.compat.StreamCodec;
import com.github.touhoumaidaffection.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KissCarryRequestPayload(int reserved) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KissCarryRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "kiss_carry_request"));

    public static final StreamCodec<ByteBuf, KissCarryRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KissCarryRequestPayload::reserved,
            KissCarryRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

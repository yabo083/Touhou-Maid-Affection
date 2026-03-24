package com.github.touhoumaidaffection.network.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RescueSoundReloadPayload(String reason) implements CustomPacketPayload {
    public static final Type<RescueSoundReloadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "rescue_sound_reload"));

    public static final StreamCodec<ByteBuf, RescueSoundReloadPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RescueSoundReloadPayload::reason,
            RescueSoundReloadPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

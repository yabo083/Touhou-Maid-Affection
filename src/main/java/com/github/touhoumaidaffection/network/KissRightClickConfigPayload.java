package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KissRightClickConfigPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<KissRightClickConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "kiss_right_click_config"));
    public static final StreamCodec<ByteBuf, KissRightClickConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            KissRightClickConfigPayload::enabled,
            KissRightClickConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

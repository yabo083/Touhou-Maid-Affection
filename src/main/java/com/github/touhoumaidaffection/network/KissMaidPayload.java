package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KissMaidPayload(int maidEntityId, int playerEntityId, boolean allowFovZoom) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KissMaidPayload> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "kiss_maid"));

    public static final StreamCodec<ByteBuf, KissMaidPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KissMaidPayload::maidEntityId,
            ByteBufCodecs.VAR_INT, KissMaidPayload::playerEntityId,
            ByteBufCodecs.BOOL, KissMaidPayload::allowFovZoom,
            KissMaidPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

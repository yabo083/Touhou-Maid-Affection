package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KissTargetedMaidRequestPayload(int maidEntityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KissTargetedMaidRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "kiss_targeted_maid_request"));

    public static final StreamCodec<ByteBuf, KissTargetedMaidRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KissTargetedMaidRequestPayload::maidEntityId,
            KissTargetedMaidRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

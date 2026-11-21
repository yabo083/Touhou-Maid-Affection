package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import com.github.touhoumaidaffection.compat.ByteBufCodecs;
import com.github.touhoumaidaffection.compat.StreamCodec;
import com.github.touhoumaidaffection.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KissTargetedMaidRequestPayload(int maidEntityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KissTargetedMaidRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "kiss_targeted_maid_request"));

    public static final StreamCodec<ByteBuf, KissTargetedMaidRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KissTargetedMaidRequestPayload::maidEntityId,
            KissTargetedMaidRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaidRescuePopPayload(
        String maidUuid,
        String maidModelId,
        String maidDisplayName,
        String ysmModelId,
        String ysmModelTexture,
        String ysmDisplayName,
        String rescueActionId
) implements CustomPacketPayload {
    public static final Type<MaidRescuePopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "maid_rescue_pop"));

    public static final StreamCodec<ByteBuf, MaidRescuePopPayload> STREAM_CODEC = StreamCodec.of(
            MaidRescuePopPayload::encode,
            MaidRescuePopPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, MaidRescuePopPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.maidUuid());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.maidModelId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.maidDisplayName());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmModelId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmModelTexture());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmDisplayName());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueActionId());
    }

    private static MaidRescuePopPayload decode(ByteBuf buf) {
        return new MaidRescuePopPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf)
        );
    }
}

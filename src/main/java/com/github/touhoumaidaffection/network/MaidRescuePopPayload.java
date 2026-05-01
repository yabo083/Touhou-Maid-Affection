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
        String maidSoundPackId,
        String ysmModelId,
        String ysmModelTexture,
        String ysmDisplayName,
        String rescueActionId,
        String rescueVoiceSourceMode,
        String rescueVoiceTlmMode,
        String rescueVoiceTlmGroup,
        String rescueVoiceTlmClip,
        String rescueVoiceSelectedId,
        String rescueSoundEventId,
        String dataPackVoiceFileName,
        byte[] dataPackVoiceData
) implements CustomPacketPayload {
    public static final Type<MaidRescuePopPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "maid_rescue_pop"));

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
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.maidSoundPackId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmModelId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmModelTexture());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.ysmDisplayName());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueActionId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueVoiceSourceMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueVoiceTlmMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueVoiceTlmGroup());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueVoiceTlmClip());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueVoiceSelectedId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.rescueSoundEventId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.dataPackVoiceFileName());
        ByteBufCodecs.BYTE_ARRAY.encode(buf, payload.dataPackVoiceData());
    }

    private static MaidRescuePopPayload decode(ByteBuf buf) {
        return new MaidRescuePopPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BYTE_ARRAY.decode(buf)
        );
    }
}

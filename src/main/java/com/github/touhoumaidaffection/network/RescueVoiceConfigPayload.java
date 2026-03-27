package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RescueVoiceConfigPayload(
        UUID maidUuid,
        String sourceMode,
        String tlmPlayMode,
        String tlmSelectedGroup,
        String tlmSelectedClip,
        String customPlayMode,
        String fixedFile,
        boolean useCommonFallback
) implements CustomPacketPayload {
    public static final Type<RescueVoiceConfigPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_voice_config"));

    public static final StreamCodec<ByteBuf, RescueVoiceConfigPayload> STREAM_CODEC = StreamCodec.of(
            RescueVoiceConfigPayload::encode,
            RescueVoiceConfigPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueVoiceConfigPayload payload) {
        ByteBufCodecs.UUID.encode(buf, payload.maidUuid());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.sourceMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.tlmPlayMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.tlmSelectedGroup());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.tlmSelectedClip());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.customPlayMode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.fixedFile());
        ByteBufCodecs.BOOL.encode(buf, payload.useCommonFallback());
    }

    private static RescueVoiceConfigPayload decode(ByteBuf buf) {
        return new RescueVoiceConfigPayload(
                ByteBufCodecs.UUID.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BOOL.decode(buf)
        );
    }
}

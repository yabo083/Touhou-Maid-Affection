package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VoicePreviewRequestPayload(
        UUID maidUuid,
        String feature,
        String voiceId
) implements CustomPacketPayload {
    public static final String FEATURE_MORNING_KISS = "morning_kiss";
    public static final String FEATURE_EMERGENCY_RESCUE = "emergency_rescue";

    public static final Type<VoicePreviewRequestPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "voice_preview_request"));

    public static final StreamCodec<ByteBuf, VoicePreviewRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.UUID, VoicePreviewRequestPayload::maidUuid,
            ByteBufCodecs.STRING_UTF8, VoicePreviewRequestPayload::feature,
            ByteBufCodecs.STRING_UTF8, VoicePreviewRequestPayload::voiceId,
            VoicePreviewRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

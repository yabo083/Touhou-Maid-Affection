package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningKissVoiceConfigPayload(
        UUID maidUuid,
        String mode,
        String selectedGroup,
        String selectedClip,
        String soundPackId
) implements CustomPacketPayload {
    public static final Type<MorningKissVoiceConfigPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "morning_kiss_voice_config"));

    public static final StreamCodec<ByteBuf, MorningKissVoiceConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.UUID, MorningKissVoiceConfigPayload::maidUuid,
            ByteBufCodecs.STRING_UTF8, MorningKissVoiceConfigPayload::mode,
            ByteBufCodecs.STRING_UTF8, MorningKissVoiceConfigPayload::selectedGroup,
            ByteBufCodecs.STRING_UTF8, MorningKissVoiceConfigPayload::selectedClip,
            ByteBufCodecs.STRING_UTF8, MorningKissVoiceConfigPayload::soundPackId,
            MorningKissVoiceConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningKissVoicePlayPayload(
        int maidEntityId,
        UUID maidUuid,
        String soundPackId,
        String mode,
        String selectedGroup,
        String selectedClip
) implements CustomPacketPayload {
    public static final Type<MorningKissVoicePlayPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "morning_kiss_voice_play"));

    public static final StreamCodec<ByteBuf, MorningKissVoicePlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MorningKissVoicePlayPayload::maidEntityId,
            UUIDUtil.STREAM_CODEC, MorningKissVoicePlayPayload::maidUuid,
            ByteBufCodecs.STRING_UTF8, MorningKissVoicePlayPayload::soundPackId,
            ByteBufCodecs.STRING_UTF8, MorningKissVoicePlayPayload::mode,
            ByteBufCodecs.STRING_UTF8, MorningKissVoicePlayPayload::selectedGroup,
            ByteBufCodecs.STRING_UTF8, MorningKissVoicePlayPayload::selectedClip,
            MorningKissVoicePlayPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

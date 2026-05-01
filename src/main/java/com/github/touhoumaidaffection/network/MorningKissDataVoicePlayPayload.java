package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningKissDataVoicePlayPayload(
        int maidEntityId,
        UUID maidUuid,
        String fileName,
        byte[] data
) implements CustomPacketPayload {
    public static final Type<MorningKissDataVoicePlayPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "morning_kiss_data_voice_play"));

    public static final StreamCodec<ByteBuf, MorningKissDataVoicePlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MorningKissDataVoicePlayPayload::maidEntityId,
            UUIDUtil.STREAM_CODEC, MorningKissDataVoicePlayPayload::maidUuid,
            ByteBufCodecs.STRING_UTF8, MorningKissDataVoicePlayPayload::fileName,
            ByteBufCodecs.BYTE_ARRAY, MorningKissDataVoicePlayPayload::data,
            MorningKissDataVoicePlayPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

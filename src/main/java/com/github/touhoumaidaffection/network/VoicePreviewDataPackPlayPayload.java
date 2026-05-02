package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VoicePreviewDataPackPlayPayload(
        int maidEntityId,
        UUID maidUuid,
        String feature,
        String fileName,
        byte[] data
) implements CustomPacketPayload {
    public static final Type<VoicePreviewDataPackPlayPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "voice_preview_data_pack_play"));

    public static final StreamCodec<ByteBuf, VoicePreviewDataPackPlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, VoicePreviewDataPackPlayPayload::maidEntityId,
            ByteBufCodecs.UUID, VoicePreviewDataPackPlayPayload::maidUuid,
            ByteBufCodecs.STRING_UTF8, VoicePreviewDataPackPlayPayload::feature,
            ByteBufCodecs.STRING_UTF8, VoicePreviewDataPackPlayPayload::fileName,
            ByteBufCodecs.BYTE_ARRAY, VoicePreviewDataPackPlayPayload::data,
            VoicePreviewDataPackPlayPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

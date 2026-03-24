package com.github.touhoumaidaffection.network.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RescueSoundSyncChunkPayload(
        String serverId,
        long generation,
        String relativePath,
        int chunkIndex,
        int totalChunks,
        int totalSize,
        byte[] chunkData
) implements CustomPacketPayload {
    public static final Type<RescueSoundSyncChunkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "rescue_sound_sync_chunk"));

    public static final StreamCodec<ByteBuf, RescueSoundSyncChunkPayload> STREAM_CODEC = StreamCodec.of(
            RescueSoundSyncChunkPayload::encode,
            RescueSoundSyncChunkPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueSoundSyncChunkPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.serverId());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.generation());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.relativePath());
        ByteBufCodecs.VAR_INT.encode(buf, payload.chunkIndex());
        ByteBufCodecs.VAR_INT.encode(buf, payload.totalChunks());
        ByteBufCodecs.VAR_INT.encode(buf, payload.totalSize());
        ByteBufCodecs.BYTE_ARRAY.encode(buf, payload.chunkData());
    }

    private static RescueSoundSyncChunkPayload decode(ByteBuf buf) {
        return new RescueSoundSyncChunkPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.BYTE_ARRAY.decode(buf)
        );
    }
}

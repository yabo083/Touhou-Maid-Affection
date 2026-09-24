package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server request for the read-only Morning Kiss AI status.
 *
 * <p>Carries no data: the server always answers with the status of the maids owned by the
 * requesting player.
 */
public record TmaAiStatusRequestPayload() implements CustomPacketPayload {
    public static final Type<TmaAiStatusRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_ai_status_request"));

    public static final StreamCodec<ByteBuf, TmaAiStatusRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new TmaAiStatusRequestPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
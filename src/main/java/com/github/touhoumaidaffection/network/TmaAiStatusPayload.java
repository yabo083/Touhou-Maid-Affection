package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client push of the read-only Morning Kiss AI status.
 *
 * <p>{@code maids} only contains maids owned by the receiving player; the remaining counters describe
 * the whole server cache. {@code canClear} reports whether the receiver may clear cache entries
 * (operator permission level 2).
 */
public record TmaAiStatusPayload(TmaAiStatusWire.Status status) implements CustomPacketPayload {
    public static final Type<TmaAiStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_ai_status"));

    public static final StreamCodec<ByteBuf, TmaAiStatusPayload> STREAM_CODEC = StreamCodec.of(
            TmaAiStatusPayload::encode,
            TmaAiStatusPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, TmaAiStatusPayload payload) {
        TmaAiStatusWire.writeStatus(TmaAiStatusByteBuf.sink(buf), payload.status());
    }

    private static TmaAiStatusPayload decode(ByteBuf buf) {
        return new TmaAiStatusPayload(TmaAiStatusWire.readStatus(TmaAiStatusByteBuf.source(buf)));
    }
}
package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server request to clear cached Morning Kiss AI lines.
 *
 * <p>{@code scope} is {@code ALL} (whole cache), {@code MAID} (one maid) or {@code POOL} (one maid and
 * one dialogue pool). The server requires operator permission level 2 for every scope and reuses the
 * same service methods as {@code /tma morning_kiss clear_ai_cache}.
 */
public record TmaAiCacheClearPayload(TmaAiStatusWire.ClearRequest request) implements CustomPacketPayload {
    public static final Type<TmaAiCacheClearPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_ai_cache_clear"));

    public static final StreamCodec<ByteBuf, TmaAiCacheClearPayload> STREAM_CODEC = StreamCodec.of(
            TmaAiCacheClearPayload::encode,
            TmaAiCacheClearPayload::decode
    );

    public TmaAiCacheClearPayload {
        request = request == null ? new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.ALL, "", "") : request;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, TmaAiCacheClearPayload payload) {
        TmaAiStatusWire.writeClear(TmaAiStatusByteBuf.sink(buf), payload.request());
    }

    private static TmaAiCacheClearPayload decode(ByteBuf buf) {
        return new TmaAiCacheClearPayload(TmaAiStatusWire.readClear(TmaAiStatusByteBuf.source(buf)));
    }
}
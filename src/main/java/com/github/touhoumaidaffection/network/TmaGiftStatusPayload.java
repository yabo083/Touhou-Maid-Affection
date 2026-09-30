package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TmaGiftStatusPayload(TmaGiftStatusWire.Status status) implements CustomPacketPayload {
    public static final Type<TmaGiftStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_gift_status"));
    public static final StreamCodec<ByteBuf, TmaGiftStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> TmaGiftStatusWire.writeStatus(TmaGiftStatusByteBuf.sink(buf), payload.status()),
            buf -> {
                if (buf.readableBytes() > 1_048_576) {
                    throw new IllegalArgumentException("Gift status packet too large");
                }
                return new TmaGiftStatusPayload(TmaGiftStatusWire.readStatus(TmaGiftStatusByteBuf.source(buf)));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

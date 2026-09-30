package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** No owner selector: the server derives ownership solely from the sending player. */
public record TmaGiftStatusRequestPayload(int page) implements CustomPacketPayload {
    public TmaGiftStatusRequestPayload { TmaGiftStatusWire.validatePage(page); }
    public static final Type<TmaGiftStatusRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_gift_status_request"));
    public static final StreamCodec<ByteBuf, TmaGiftStatusRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeInt(payload.page()),
            buf -> new TmaGiftStatusRequestPayload(buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

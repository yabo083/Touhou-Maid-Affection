package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client to server settings request.
 *
 * <p>An empty entry list asks for the current authoritative state only; a non-empty list asks the
 * server to apply those values. The server validates every entry and rejects the whole packet when
 * one of them is invalid, so a request never applies partially.
 */
public record TmaSettingsRequestPayload(List<TmaSettingsWire.Entry> entries) implements CustomPacketPayload {
    public static final Type<TmaSettingsRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_settings_request"));

    public static final StreamCodec<ByteBuf, TmaSettingsRequestPayload> STREAM_CODEC = StreamCodec.of(
            TmaSettingsRequestPayload::encode,
            TmaSettingsRequestPayload::decode
    );

    public TmaSettingsRequestPayload {
        entries = TmaSettingsWire.sanitize(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, TmaSettingsRequestPayload payload) {
        TmaSettingsWire.writeEntries(TmaSettingsByteBuf.sink(buf), payload.entries());
    }

    private static TmaSettingsRequestPayload decode(ByteBuf buf) {
        return new TmaSettingsRequestPayload(TmaSettingsWire.readEntries(TmaSettingsByteBuf.source(buf)));
    }
}